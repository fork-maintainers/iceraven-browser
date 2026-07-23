#!/usr/bin/env python3
"""
effpretty — accessible viewer for ui/efficiency structured test logs.

A single, portable Python tool (no shell wrappers) with two subcommands:

  effpretty view [FILE]                 Render a saved logcat / downloaded Firebase
                                        artifact (or stdin). No device needed.
  effpretty capture [--mode MODE]       Capture from a connected device via adb and
                                        render live. MODE is one of:
                                          live  (default) clear buffer, then follow
                                          watch           follow WITHOUT clearing (attach mid-run)
                                          dump            snapshot the current buffer and exit

WHAT IT DOES
------------
Finds the harness's structured `[LEVEL] glyph message (timing)` lines in the logcat
stream and renders them with:
  - indentation preserved (STEP > CMD > LOC hierarchy),
  - the glyphs the harness emits (★ ➤ 🔎 ✔ ✖ • ⚠️),
  - a COLORBLIND-SAFE palette (Okabe–Ito; truecolor with 256-color auto-fallback),
applied as a *secondary* cue on top of glyph + indent, never as the only signal.

Color lives HERE, at view time — the harness emits plain text on the `Eff` logcat
tag. One plain structured source, many renderers (this CLI today, a GUI later), and
clean machine-parseable artifacts. `--out` additionally writes an ANSI-free copy for
handing to a person or an AI agent.

WHY PYTHON, NOT SHELL
---------------------
Portable (Linux CI, Windows, macOS), no shell-quoting/bashism traps, one artifact to
test and maintain, and a single `--help`-introspectable interface. The only CI-relevant
path (`view` over a downloaded artifact) needs no device or shell at all.

USAGE EXAMPLES
--------------
  effpretty view firebase_logcat.txt
  effpretty view < saved.txt
  effpretty capture --out ~/Desktop/run.txt           # live: clear + follow
  effpretty capture --mode watch --out run.txt         # attach to a run in progress
  effpretty capture --mode dump  --out run.txt          # snapshot a finished run
"""

from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys

# --- Okabe–Ito colorblind-safe palette (R, G, B) --------------------------------
# https://jfly.uni-koeln.de/color/ — distinguishable across the common
# deuteran/protan (red-green) and tritan (blue-yellow) types.
OKABE_ITO = {
    "black": (0, 0, 0),
    "orange": (230, 159, 0),
    "sky": (86, 180, 233),
    "green": (0, 158, 115),  # bluish green — used instead of pure green
    "yellow": (240, 228, 66),
    "blue": (0, 114, 178),
    "vermillion": (213, 94, 0),  # used instead of pure red for failures
    "purple": (204, 121, 167),
    "gray": (140, 140, 140),
}

# Nearest xterm-256 indices, for terminals without 24-bit truecolor (e.g. macOS
# Terminal.app). Keeps the same colorblind-safe intent at lower fidelity.
OKABE_256 = {
    "orange": 214,
    "sky": 75,
    "green": 36,
    "yellow": 227,
    "blue": 32,
    "vermillion": 166,
    "purple": 175,
    "gray": 244,
    "black": 16,
}

# Level -> (color key, bold). Glyph + indent carry meaning; color is the extra cue.
LEVEL_STYLE = {
    "STEP": ("blue", True),
    "CMD": ("sky", False),
    "LOC": ("purple", False),
    "OK": ("green", False),
    "ERR": ("vermillion", True),
    "WARN": ("orange", True),
    "INFO": ("gray", False),
    "SHOT": ("sky", False),
    "TEST": ("blue", True),
}

# Dedicated tags + silence everything else (*:S). No shell = no glob/quoting trap on "*:S".
# Structured stream is on "Eff"; "System.out" kept for logs from before the consolidation.
LOGCAT_TAGS = [
    "Eff:I",
    "TestRunner:I",
    "System.out:I",
    "EffScreenDump:I",
    "BaseTest:I",
    "PageNavigation:I",
    "AndroidRuntime:E",
    "*:S",
]

ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")
# logcat `-v threadtime`: "MM-DD HH:MM:SS.mmm  PID  TID  L Tag: payload"
LOGCAT_RE = re.compile(
    r"^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}(?:\.\d+)?\s+\d+\s+\d+\s+[VDIWEF]\s+([\w.$]+)\s*:\s?(.*)$"
)
# logcat `-v time`: "MM-DD HH:MM:SS.mmm L/Tag(PID): payload"
TIME_RE = re.compile(
    r"^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}(?:\.\d+)?\s+[VDIWEF]/([\w.$]+)\s*\(\s*\d+\):\s?(.*)$"
)
STRUCT_RE = re.compile(r"^(\s*)\[([A-Z]+)\](.*)$")
TIMING_RE = re.compile(r"(\(\d+(?:\.\d+)?\s*ms\))")
SLOW_RE = re.compile(r"(⚠️?\s*SLOW)")


def color_mode(stream, forced: str | None = None) -> str:
    """Return 'truecolor' | '256' | 'off'. Auto-detects unless forced."""
    if forced:
        return forced
    if os.environ.get("NO_COLOR") is not None:
        return "off"
    if not (hasattr(stream, "isatty") and stream.isatty()):
        return "off"
    ct = os.environ.get("COLORTERM", "").lower()
    if "truecolor" in ct or "24bit" in ct:
        return "truecolor"
    return "256"


class Painter:
    def __init__(self, mode: str):
        self.mode = mode  # 'truecolor' | '256' | 'off'

    def paint(
        self,
        text: str,
        color_key: str | None = None,
        bold: bool = False,
        dim: bool = False,
    ) -> str:
        if self.mode == "off" or not text:
            return text
        codes = []
        if bold:
            codes.append("1")
        if dim:
            codes.append("2")
        if color_key:
            if self.mode == "truecolor" and color_key in OKABE_ITO:
                r, g, b = OKABE_ITO[color_key]
                codes.append(f"38;2;{r};{g};{b}")
            elif color_key in OKABE_256:
                codes.append(f"38;5;{OKABE_256[color_key]}")
        if not codes:
            return text
        return f"\x1b[{';'.join(codes)}m{text}\x1b[0m"


def strip_ansi(s: str) -> str:
    return ANSI_RE.sub("", s)


def parse(line: str):
    """Return (kind, tag, payload). kind in {structured, testrunner, androidruntime, other}."""
    line = strip_ansi(line.rstrip("\n"))
    m = LOGCAT_RE.match(line)
    if m:
        tag, payload = m.group(1), m.group(2)
    else:
        m2 = TIME_RE.match(line)
        if m2:
            tag, payload = m2.group(1), m2.group(2)
        else:
            tag, payload = None, line  # already filtered/stripped

    # "Eff" is the dedicated tag the harness now emits on; "System.out" is kept for
    # older logs captured before the logging-consolidation change.
    if tag in ("Eff", "System.out", None) and STRUCT_RE.match(payload):
        return "structured", tag, payload
    if tag == "TestRunner":
        return "testrunner", tag, payload
    if tag == "AndroidRuntime":
        return "androidruntime", tag, payload
    return "other", tag, payload


def render_structured(payload: str, p: Painter, color_scope: str) -> str:
    m = STRUCT_RE.match(payload)
    indent, level, rest = m.group(1), m.group(2), m.group(3)
    color_key, bold = LEVEL_STYLE.get(level, (None, False))
    tag_txt = p.paint(f"[{level}]", color_key, bold=bold)

    if color_scope == "line":
        return f"{indent}{tag_txt}{p.paint(rest, color_key, bold=bold)}"

    body = rest
    body = SLOW_RE.sub(lambda mo: p.paint(mo.group(1), "orange", bold=True), body)
    body = TIMING_RE.sub(lambda mo: p.paint(mo.group(1), "gray", dim=True), body)
    if level == "ERR":
        body = p.paint(body, "vermillion")
    return f"{indent}{tag_txt}{body}"


def render_testrunner(payload: str, p: Painter) -> str | None:
    if payload.startswith("started:"):
        name = payload[len("started:") :].strip()
        bar = "─" * 68
        return (
            p.paint(bar, "blue")
            + "\n"
            + p.paint("▶ TEST ", "blue", bold=True)
            + p.paint(name, "blue", bold=True)
        )
    if payload.startswith("ignored:"):
        return p.paint("⊘ SKIPPED ", "orange", bold=True) + p.paint(
            payload[len("ignored:") :].strip(), "orange"
        )
    if payload.startswith("finished:"):
        return None  # redundant with the run-finished tally
    if payload.startswith("run finished:"):
        failed = re.search(r"(\d+)\s+failed", payload)
        n = int(failed.group(1)) if failed else 0
        tail = payload.split("run finished:")[1].strip()
        if n > 0:
            return p.paint(f"■ RUN FINISHED — {tail}", "vermillion", bold=True)
        return p.paint(f"■ run finished — {tail}", "green")
    return p.paint(payload, "gray", dim=True)


def render(line: str, p: Painter, color_scope: str) -> str | None:
    kind, tag, payload = parse(line)
    if kind == "structured":
        return render_structured(payload, p, color_scope)
    if kind == "testrunner":
        return render_testrunner(payload, p)
    if kind == "androidruntime":
        return p.paint("‼ CRASH  ", "vermillion", bold=True) + p.paint(
            payload, "vermillion"
        )
    if "caught, retrying" in payload:
        return p.paint(
            "↻ RETRY (first attempt failed) — " + payload, "orange", bold=True
        )
    return p.paint(payload, "gray", dim=True)


def plain(line: str) -> str | None:
    """Clean text for the handoff file: structured lines + banners, ANSI stripped."""
    kind, tag, payload = parse(line)
    if kind == "structured":
        return payload
    if kind == "testrunner":
        return None if payload.startswith("finished:") else payload
    if kind == "androidruntime":
        return "CRASH: " + payload
    if "caught, retrying" in payload:
        return "RETRY (first attempt failed): " + payload
    return payload


def process(lines, painter: Painter, color_scope: str, out_f) -> None:
    for raw in lines:
        colored = render(raw, painter, color_scope)
        if colored is not None:
            print(colored, flush=True)
        if out_f is not None:
            pl = plain(raw)
            if pl is not None:
                out_f.write(pl + "\n")
                out_f.flush()


# --- capture (adb) --------------------------------------------------------------


def find_adb() -> str | None:
    candidates = [
        os.path.join(os.environ.get("ANDROID_SDK_ROOT", ""), "platform-tools", "adb"),
        os.path.join(os.environ.get("ANDROID_HOME", ""), "platform-tools", "adb"),
        shutil.which("adb") or "",
        os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"),
    ]
    for c in candidates:
        if c and os.path.isfile(c) and os.access(c, os.X_OK):
            return c
    return None


def capture_lines(mode: str):
    """Yield logcat lines from a connected device. mode: live | watch | dump."""
    adb = find_adb()
    if not adb:
        sys.stderr.write(
            "effpretty: adb not found. Set ANDROID_SDK_ROOT or put adb on PATH.\n"
        )
        sys.exit(1)
    if mode == "live":
        subprocess.run([adb, "logcat", "-c"], check=False)
    cmd = [adb, "logcat", "-v", "time"]
    if mode == "dump":
        cmd.append("-d")
    cmd += LOGCAT_TAGS  # "*:S" passed literally; no shell means no glob expansion
    proc = subprocess.Popen(
        cmd, stdout=subprocess.PIPE, text=True, bufsize=1, errors="replace"
    )
    try:
        assert proc.stdout is not None
        yield from proc.stdout
    finally:
        if proc.poll() is None:
            proc.terminate()
            try:
                proc.wait(timeout=3)
            except subprocess.TimeoutExpired:
                proc.kill()


# --- CLI ------------------------------------------------------------------------


def add_common(sp):
    sp.add_argument(
        "--out",
        metavar="FILE",
        help="also write a clean, ANSI-free copy here (for handoff).",
    )
    sp.add_argument(
        "--color-scope",
        choices=["tag", "line"],
        default="tag",
        help="'tag' (default) colors the level tag, keeps message high-contrast; "
        "'line' colors the whole line.",
    )
    sp.add_argument("--no-color", action="store_true", help="disable terminal color.")
    sp.add_argument(
        "--color-mode",
        choices=["auto", "truecolor", "256", "off"],
        default="auto",
        help="color depth; 'auto' uses truecolor only if the terminal advertises it "
        "(COLORTERM), else 256-color.",
    )


def build_parser() -> argparse.ArgumentParser:
    ap = argparse.ArgumentParser(
        prog="effpretty", description="Accessible viewer for ui/efficiency test logs."
    )
    sub = ap.add_subparsers(dest="cmd")

    pv = sub.add_parser(
        "view", help="render a saved logcat/artifact (file arg) or stdin."
    )
    pv.add_argument(
        "path", nargs="?", help="log file to render; if omitted, reads stdin."
    )
    add_common(pv)

    pc = sub.add_parser(
        "capture", help="capture from a device via adb and render live."
    )
    pc.add_argument(
        "--mode",
        choices=["live", "watch", "dump"],
        default="live",
        help="live=clear+follow (default), watch=follow without clearing, dump=snapshot.",
    )
    add_common(pc)
    return ap


def main() -> int:
    argv = sys.argv[1:]
    # Default to `view` when no subcommand is given (e.g. `effpretty run.txt` or piped stdin).
    if not argv or (argv[0] not in ("view", "capture", "-h", "--help")):
        argv = ["view"] + argv

    args = build_parser().parse_args(argv)

    forced = None
    if args.no_color or args.color_mode == "off":
        forced = "off"
    elif args.color_mode != "auto":
        forced = args.color_mode
    painter = Painter(color_mode(sys.stdout, forced))

    out_f = open(args.out, "w", encoding="utf-8") if args.out else None
    close_src = None
    try:
        if args.cmd == "capture":
            lines = capture_lines(args.mode)
        elif getattr(args, "path", None):
            close_src = open(args.path, encoding="utf-8", errors="replace")
            lines = close_src
        else:
            lines = sys.stdin
        process(lines, painter, args.color_scope, out_f)
    except (BrokenPipeError, KeyboardInterrupt):
        pass
    finally:
        if out_f is not None:
            out_f.close()
        if close_src is not None:
            close_src.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
