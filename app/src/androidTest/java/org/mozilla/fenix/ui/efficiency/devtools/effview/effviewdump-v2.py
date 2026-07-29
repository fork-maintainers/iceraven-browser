#!/usr/bin/env python3
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
effviewdump v2 - Enhanced with overlapping element detection, smart ranking, and code preview
"""

import base64
import json
import sys


def _bounds(b):
    """Parse bounds string [x1,y1][x2,y2] -> (x, y, w, h)"""
    import re

    m = re.findall(r"\[(\d+),(\d+)\]", b or "")
    if len(m) != 2:
        return None
    (x1, y1), (x2, y2) = m
    return int(x1), int(y1), int(x2) - int(x1), int(y2) - int(y1)


def bounds_overlap(b1, b2, threshold=0.8):
    """Check if two bounds overlap significantly (>threshold intersection over union)"""
    if not b1 or not b2:
        return False
    x1, y1, w1, h1 = b1
    x2, y2, w2, h2 = b2
    # Calculate intersection
    xi = max(x1, x2)
    yi = max(y1, y2)
    xi2 = min(x1 + w1, x2 + w2)
    yi2 = min(y1 + h1, y2 + h2)
    if xi >= xi2 or yi >= yi2:
        return False
    intersection = (xi2 - xi) * (yi2 - yi)
    union = w1 * h1 + w2 * h2 - intersection
    return (intersection / union) >= threshold if union > 0 else False


def rank_selector(layer, strategy):
    """Rank selector quality: lower is better. Compose > Espresso > UIAutomator, testTag > res-id > desc > text"""
    layer_rank = {"compose": 0, "espresso": 1, "uiautomator": 2}.get(layer, 3)

    # Strategy priority: testTag, resource-id, content-desc, text
    if "TAG" in strategy.upper() or "TESTTAG" in strategy.upper():
        strat_rank = 0
    elif "RES" in strategy.upper() or "ID" in strategy.upper():
        strat_rank = 1
    elif (
        "CONTENT_DESC" in strategy.upper() or "CONTENT-DESCRIPTION" in strategy.upper()
    ):
        strat_rank = 2
    elif "TEXT" in strategy.upper():
        strat_rank = 3
    else:
        strat_rank = 4

    return layer_rank * 10 + strat_rank


def detect_page(elements):
    """Detect which page we're on based on visible element patterns (dynamic + static fallbacks)"""
    # Collect element prefixes to infer page
    prefixes = {}
    for e in elements:
        tag = e.get("testTag", "") or e.get("resId", "")
        if tag and "." in tag:
            # Extract prefix (e.g., "homepage.view" → "homepage")
            prefix = tag.split(".")[0].lower()
            # Skip generic prefixes
            if prefix not in ["compose", "mozac", "android", "debug"]:
                prefixes[prefix] = prefixes.get(prefix, 0) + 1

    # Find most common prefix (likely indicates the page)
    if prefixes:
        most_common = max(prefixes.items(), key=lambda x: x[1])
        if most_common[1] >= 3:  # At least 3 elements with this prefix
            page_name = most_common[0].capitalize()
            # Handle special cases
            if page_name == "Homepage":
                return "HomePage"
            if page_name in ["Browser", "Mozac_browser", "Browserwrapper"]:
                return "BrowserPage"
            if page_name in ["Settings", "Quicksettings"]:
                return "SettingsPage"
            if page_name in ["Customtab", "Custom_tab"]:
                return "CustomTabsPage"
            return page_name + "Page"  # Default: prefix + "Page"

    # Static fallbacks for edge cases
    tags = {(e.get("testTag", "") or e.get("resId", "")).lower() for e in elements}
    if "homepage.view" in tags:
        return "HomePage"
    if "browserWrapper" in tags:
        return "BrowserPage"

    return "UnknownPage"


def load_and_group(json_path):
    """Load JSON and group overlapping elements"""
    d = json.load(open(json_path, encoding="utf-8"))
    elements = d.get("elements", [])

    # Add bounds and index to each element
    for i, e in enumerate(elements):
        e["_idx"] = i
        b = e.get("bounds", [])
        e["_bounds_parsed"] = (
            (b[0], b[1], b[2] - b[0], b[3] - b[1]) if len(b) == 4 else None
        )

    # Group elements by overlapping bounds
    groups = []
    used = set()

    for i, e1 in enumerate(elements):
        if i in used or not e1["_bounds_parsed"]:
            continue

        group = [e1]
        used.add(i)

        for j, e2 in enumerate(elements[i + 1 :], i + 1):
            if j not in used and e2["_bounds_parsed"]:
                if bounds_overlap(e1["_bounds_parsed"], e2["_bounds_parsed"]):
                    group.append(e2)
                    used.add(j)

        groups.append(group)

    return d, groups


def render_v2(json_path, png_path, out_html):
    """Render enhanced overlay with overlap detection and smart ranking"""
    d, groups = load_and_group(json_path)
    W = d.get("screen", {}).get("width", 1080)
    H = d.get("screen", {}).get("height", 2400)
    page = detect_page(d.get("elements", []))

    data = base64.b64encode(open(png_path, "rb").read()).decode()

    # Rank groups by smallest area (for z-index)
    groups_with_area = []
    for group in groups:
        e = group[0]  # Representative element
        b = e["_bounds_parsed"]
        if b:
            area = b[2] * b[3]
            groups_with_area.append((group, b, area))

    groups_with_area.sort(key=lambda x: x[2], reverse=True)

    boxes = []
    group_data = []  # Store group info for JavaScript

    for rank, (group, bounds, area) in enumerate(groups_with_area):
        gid = rank
        x, y, w, h = bounds
        z = rank + 1

        # Rank selectors in group
        ranked = []
        for e in group:
            layer = e.get("layer", "uiautomator")
            strategy = e.get("strategy", "")
            score = rank_selector(layer, strategy)
            ranked.append((score, e))

        ranked.sort(key=lambda x: x[0])
        best = ranked[0][1]

        # Color by best layer
        color = {
            "compose": "#0B7285",
            "espresso": "#2563eb",
            "uiautomator": "#8a5a00",
        }.get(best.get("layer"), "#0B7285")

        # Create box
        boxes.append(
            f'<div class="bx" style="left:{x / W * 100:.3f}%;top:{y / H * 100:.3f}%;'
            f'width:{w / W * 100:.3f}%;height:{h / H * 100:.3f}%;border-color:{color};z-index:{z}" '
            f'onclick="showGroup({gid})"></div>'
        )

        # Store group data for detail panel
        group_info = []
        for score, e in ranked:
            layer = e.get("layer", "uiautomator")
            label = (
                e.get("text")
                or e.get("contentDesc")
                or e.get("testTag")
                or e.get("resId")
                or "(unlabeled)"
            )
            strategy = e.get("strategy", "")
            value = e.get("value", "")
            clickable = bool(e.get("clickable"))
            classname = e.get("class", "").lower()
            text_input = (
                "edit" in classname or "textfield" in classname or "input" in classname
            )

            # Generate snippet with groups field
            strat_name = strategy.split()[0].replace("/", "_").replace("-", "_")
            snippet = f'''val MY_ELEMENT = Selector(
    strategy = SelectorStrategy.{strat_name},
    value = "{value}",
    description = "{label}",
    groups = listOf(),  // e.g., "toolbar", "mainMenu", "requiredForPage"
)'''

            group_info.append({
                "layer": layer,
                "label": label,
                "strategy": strategy,
                "value": value,
                "clickable": clickable,
                "textInput": text_input,
                "snippet": snippet,
                "rank": score,
                "recommended": score == ranked[0][0],
            })

        group_data.append(group_info)

    # Generate HTML
    html_content = f"""<!doctype html><meta charset=utf-8><title>effview v2</title>
<style>
body{{margin:0;display:flex;height:100vh;font:14px -apple-system,Arial}}
#img{{position:relative;flex:0 0 auto;height:100vh}} #img img{{height:100vh;display:block}}
.bx{{position:absolute;border:1.5px solid;background:rgba(11,114,133,.08);cursor:pointer;transition:all .15s}}
.bx:hover,.bx.sel{{border-width:2.5px;border-color:#C0392B!important;background:rgba(192,57,43,.18)}}
#side{{flex:1;overflow:auto;padding:16px;border-left:1px solid #ddd}}
#side h2{{font-size:16px;color:#1E3A5F;margin:0 0 12px}}
#detail{{display:none;margin-top:12px;padding:12px;border:1px solid #ddd;border-radius:8px;background:#f9fafb}}
#detail.show{{display:block}}
#detail h3{{margin:0 0 8px;font-size:14px;color:#1E3A5F}}
.selector-opt{{padding:10px;margin:8px 0;border:1px solid #e5e7eb;border-radius:6px;background:#fff;cursor:pointer}}
.selector-opt:hover{{background:#f3f4f6;border-color:#9ca3af}}
.selector-opt.rec{{border-color:#10b981;border-width:2px;background:#ecfdf5}}
.badge{{display:inline-block;font-size:10px;padding:2px 6px;border-radius:4px;font-weight:700;text-transform:uppercase;color:#fff}}
.badge.compose{{background:#0B7285}} .badge.espresso{{background:#2563eb}} .badge.uiautomator{{background:#8a5a00}}
.badge.rec{{background:#10b981;margin-left:6px}}
.snippet{{background:#1f2937;color:#e5e7eb;padding:8px;border-radius:4px;font:12px monospace;margin:8px 0;white-space:pre;overflow-x:auto}}
.file{{color:#059669;font-weight:600;font-size:12px;margin:4px 0}}
button{{padding:6px 12px;background:#2563eb;color:#fff;border:none;border-radius:4px;cursor:pointer;font-size:12px}}
button:hover{{background:#1d4ed8}}
.copied{{background:#10b981!important}}
.list-item{{padding:8px;margin:4px 0;border:1px solid #e5e7eb;border-radius:4px;background:#fff;cursor:pointer;font-size:12px}}
.list-item:hover{{background:#f3f4f6;border-color:#9ca3af}}
</style>
<div id="img">{"".join(boxes)}<img src="data:image/png;base64,{data}"></div>
<div id="side">
<h2>Element Inspector</h2>
<p style="color:#667;font-size:12px;margin:0 0 12px">Detected page: <b>{page}</b> → Paste selectors in <code>{page}Selectors.kt</code></p>
<p style="color:#667;font-size:12px;margin:0 0 12px">Click any element on screen to see all matching selectors, ranked by preference.</p>
<div id="detail">
<h3>Available Selectors</h3>
<div id="opts"></div>
</div>
<div style="margin-top:24px;padding-top:16px;border-top:1px solid #e5e7eb">
<h3 style="cursor:pointer;user-select:none;color:#1E3A5F;font-size:14px;margin:0 0 12px" onclick="toggleList()">▼ All Selectors ({len(d.get("elements", []))})</h3>
<div id="allList" style="display:none">
<input type="text" id="search" placeholder="🔍 Search..." style="width:100%;padding:6px 8px;border:1px solid #ddd;border-radius:4px;font-size:12px;margin-bottom:8px">
<div style="margin-bottom:12px">
<label style="font-size:12px;margin-right:12px;cursor:pointer"><input type="checkbox" id="filterClick"> Show only clickable (🖱️)</label>
</div>
<div id="fullList" style="max-height:400px;overflow-y:auto"></div>
</div>
</div>
</div>
<script>
const groups = {json.dumps(group_data)};
// Build allSelectors with group index for back-reference
const allSelectors = [];
groups.forEach((group, gid) => {{
  group.forEach(sel => {{
    allSelectors.push({{ ...sel, _groupId: gid }});
  }});
}});
let curBox = null;

function showGroup(gid) {{
  if (curBox) curBox.classList.remove('sel');
  curBox = document.querySelectorAll('.bx')[gid];
  if (curBox) curBox.classList.add('sel');

  const group = groups[gid];
  const detail = document.getElementById('detail');
  const opts = document.getElementById('opts');
  opts.innerHTML = '';

  group.forEach((sel, idx) => {{
    const opt = document.createElement('div');
    opt.className = 'selector-opt' + (sel.recommended ? ' rec' : '');
    opt.innerHTML = `
      <div>
        <span class="badge ${{sel.layer}}">${{sel.layer}}</span>
        ${{sel.recommended ? '<span class="badge rec">⭐ Recommended</span>' : ''}}
        ${{sel.clickable ? ' 🖱' : ''}}
      </div>
      <div style="margin:6px 0"><b>${{sel.label}}</b></div>
      <div style="font-size:12px;color:#6b7280">${{sel.strategy}}</div>
      <div class="snippet">${{sel.snippet}}</div>
      <button onclick="copySnippet(event, ${{gid}}, ${{idx}})">📋 Copy Code</button>
    `;
    opts.appendChild(opt);
  }});

  detail.classList.add('show');
  detail.scrollIntoView({{behavior: 'smooth', block: 'nearest'}});
}}

function copySnippet(e, gid, idx) {{
  const snippet = groups[gid][idx].snippet;
  navigator.clipboard.writeText(snippet);
  const btn = e.target;
  btn.textContent = '✓ Copied!';
  btn.classList.add('copied');
  setTimeout(() => {{
    btn.textContent = '📋 Copy Code';
    btn.classList.remove('copied');
  }}, 2000);
}}

function toggleList() {{
  const list = document.getElementById('allList');
  const h3 = list.previousElementSibling;
  const show = list.style.display === 'none';
  list.style.display = show ? 'block' : 'none';
  h3.textContent = h3.textContent.replace(/^[▼▶]/, show ? '▼' : '▶');
  if (show && !list.dataset.built) {{
    buildFullList();
    list.dataset.built = 'true';
  }}
}}

function buildFullList() {{
  const container = document.getElementById('fullList');
  allSelectors.forEach((sel, idx) => {{
    const item = document.createElement('div');
    item.className = 'list-item';
    item.dataset.label = sel.label.toLowerCase();
    item.dataset.clickable = sel.clickable;
    item.dataset.textInput = sel.textInput;
    item.innerHTML = `
      <span class="badge ${{sel.layer}}">${{sel.layer}}</span>
      ${{sel.clickable ? ' 🖱' : ''}}
      <b>${{sel.label}}</b>
      <div style="font-size:11px;color:#6b7280;margin-top:2px">${{sel.strategy}}</div>
    `;
    item.onclick = () => copyFromList(idx);
    container.appendChild(item);
  }});
}}

function copyFromList(idx) {{
  const sel = allSelectors[idx];
  const gid = sel._groupId;
  // Show the group detail panel (same as clicking on screen)
  showGroup(gid);
  // Scroll the detail panel into view
  document.getElementById('detail')?.scrollIntoView({{behavior: 'smooth', block: 'nearest'}});
}}

document.getElementById('search')?.addEventListener('input', e => {{
  const q = e.target.value.toLowerCase();
  document.querySelectorAll('.list-item').forEach(item => {{
    item.style.display = item.dataset.label.includes(q) ? 'block' : 'none';
  }});
}});

document.getElementById('filterClick')?.addEventListener('change', e => {{
  applyFilters();
}});

document.getElementById('filterClick')?.addEventListener('change', e => {{
  const clickOnly = e.target.checked;
  document.querySelectorAll('.list-item').forEach(item => {{
    item.style.display = !clickOnly || item.dataset.clickable === 'true' ? 'block' : 'none';
  }});
}});
</script>"""

    open(out_html, "w", encoding="utf-8").write(html_content)
    print(
        f"wrote {out_html} ({len(groups)} elements, {len(d.get('elements', []))} selectors)"
    )


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: effviewdump-v2.py <json> <png> <out.html>")
        sys.exit(1)
    render_v2(sys.argv[1], sys.argv[2], sys.argv[3])
