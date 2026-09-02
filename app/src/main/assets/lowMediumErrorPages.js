/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

/* eslint-disable no-unsanitized/property */ /* bug 1889942 */

/**
 * Handles the parsing of the ErrorPages URI and then passes them to injectValues
 */
function parseQuery(queryString) {
  if (queryString[0] === "?") {
    queryString = queryString.substr(1);
  }
  const query = Object.fromEntries(new URLSearchParams(queryString).entries());
  injectValues(query);
  updateShowSSL(query);
  updateShowHSTS(query);
}

/**
 * Updates the HTML elements based on the queryMap
 */
function injectValues(queryMap) {
  const tryAgainButton = document.getElementById("errorTryAgain");
  const continueHttpButton = document.getElementById("continueHttp");
  const backFromHttpButton = document.getElementById("backFromHttp");

  // Go through each element and inject the values
  document.title = queryMap.title;
  tryAgainButton.innerHTML = queryMap.button;
  continueHttpButton.innerHTML = queryMap.continueHttpButton;
  backFromHttpButton.innerHTML = queryMap.badCertGoBack;
  document.getElementById("errorTitleText").innerHTML = queryMap.title;
  document.getElementById("errorShortDesc").innerHTML = queryMap.description;
  document.getElementById("advancedButton").innerHTML =
    queryMap.badCertAdvanced;
  document.getElementById("badCertTechnicalInfo").innerHTML =
    queryMap.badCertTechInfo;
  document.getElementById("advancedPanelBackButton").innerHTML =
    queryMap.badCertGoBack;
  document.getElementById("advancedPanelAcceptButton").innerHTML =
    queryMap.badCertAcceptTemporary;
  document.getElementById("advancedPanelAcceptButton").dataset.isPrivate =
    queryMap.isPrivate;

  // If no image is passed in, remove the element so as not to leave an empty iframe
  const errorImage = document.getElementById("errorImage");
  if (!queryMap.image) {
    errorImage.remove();
  } else {
    errorImage.src = "resource://android/assets/" + queryMap.image;
  }

  if (queryMap.showContinueHttp === "true") {
    // On the "HTTPS-Only" error page "Try again" doesn't make sense since reloading the page
    // will just show an error page again.
    tryAgainButton.style.display = "none";
  } else {
    continueHttpButton.style.display = "none";
    backFromHttpButton.style.display = "none";
  }

  if (queryMap.errorCode) {
    const errorCode = document.getElementById("errorCode");
    errorCode.textContent = queryMap.errorCode;
  }

  // Only offer an archived copy when we were given a cleaned http(s) URL for the
  // page that failed to load. The labels are localized and passed as query params.
  if (queryMap.archiveUrl) {
    document.getElementById("viewArchivedButton").textContent =
      queryMap.archiveCheckButtonLabel;
    document.getElementById("viewArchivedButton").style.display = "block";
    document.getElementById("archiveNotFoundText").textContent =
      queryMap.archiveNotFoundMessage;
    document.getElementById("archiveSearchWebLink").textContent =
      queryMap.archiveSearchWebLabel;
    document.getElementById("archiveUnreachableText").textContent =
      queryMap.archiveUnreachableMessage;
    document.getElementById("archiveRetryLink").textContent =
      queryMap.archiveRetryLabel;
  }
}

// Custom scheme used to hand archive actions back to native code, where the
// default search engine lives. Intercepted by AppRequestInterceptor.
const ERROR_PAGE_ACTION_PREFIX = "firefox-error-action://";

/**
 * Ask the availability API for the closest archived snapshot of the failed page
 * and open it if one exists. While the request is in flight the button shows a
 * spinner. If nothing is archived the button is replaced with a warning offering
 * a web search instead; if the archive service can't be reached the button is
 * replaced with a warning offering to retry the lookup.
 */
async function viewArchivedVersion(queryMap) {
  const button = document.getElementById("viewArchivedButton");
  button.disabled = true;
  button.textContent = "";
  const spinner = document.createElement("span");
  spinner.className = "spinner";
  button.appendChild(spinner);
  button.appendChild(document.createTextNode(queryMap.archiveCheckingLabel));
  let data;
  try {
    const response = await fetch(
      "https://archive.org/wayback/available?url=" +
        encodeURIComponent(queryMap.archiveUrl)
    );
    if (!response.ok) {
      showArchiveError(queryMap);
      return;
    }
    data = await response.json();
  } catch (e) {
    // Network failure or an unparseable response: the service is unreachable.
    showArchiveError(queryMap);
    return;
  }
  const snapshot = data?.archived_snapshots?.closest;
  if (snapshot?.available && snapshot.url) {
    window.location.href =
      ERROR_PAGE_ACTION_PREFIX + "open?url=" + encodeURIComponent(snapshot.url);
    return;
  }
  showNoArchiveFound(queryMap.archiveUrl);
}

/**
 * Hide the archive button and show a warning offering to search the web for the
 * failed page instead.
 */
function showNoArchiveFound(archiveUrl) {
  document.getElementById("viewArchivedButton").style.display = "none";
  document.getElementById("archiveWarningContent").hidden = false;
  document
    .getElementById("archiveSearchWebLink")
    .addEventListener("click", e => {
      e.preventDefault();
      searchTheWeb(archiveUrl);
    });
}

/**
 * Hide the archive button and show a warning that the archive service couldn't
 * be reached, offering to retry the lookup. Uses `onclick` (rather than an added
 * listener) so repeated failures don't stack duplicate retry handlers.
 */
function showArchiveError(queryMap) {
  document.getElementById("viewArchivedButton").style.display = "none";
  const errorContent = document.getElementById("archiveErrorContent");
  errorContent.hidden = false;
  document.getElementById("archiveRetryLink").onclick = e => {
    e.preventDefault();
    errorContent.hidden = true;
    document.getElementById("viewArchivedButton").style.display = "block";
    viewArchivedVersion(queryMap);
  };
}

/**
 * Tell native code the archive button was clicked so it can record usage telemetry,
 * regardless of the lookup outcome. Fire-and-forget: native records the event and denies
 * the navigation, so the page stays put and the availability lookup proceeds.
 */
function recordArchiveButtonClicked() {
  window.location.href = ERROR_PAGE_ACTION_PREFIX + "attempt";
}

/**
 * Hand the cleaned URL to native code as a search query (rather than an address)
 * so it is searched with the user's default search engine.
 */
function searchTheWeb(archiveUrl) {
  const query = archiveUrl.replace(/^https?:\/\//, "");
  window.location.href =
    ERROR_PAGE_ACTION_PREFIX + "search?q=" + encodeURIComponent(query);
}

let advancedVisible = false;

/**
 * Used to show or hide the "accept" button based on the validity of the SSL certificate
 */
function updateShowSSL(queryMap) {
  /** @type {'true' | 'false'} */
  const showSSL = queryMap.showSSL;
  if (typeof document.addCertException === "undefined") {
    document.getElementById("advancedButton").style.display = "none";
  } else if (showSSL === "true") {
    document.getElementById("advancedButton").style.display = "block";
  } else {
    document.getElementById("advancedButton").style.display = "none";
  }
}

/**
 * Used to show or hide the "accept" button based for the HSTS error page
 */
function updateShowHSTS(queryMap) {
  const showHSTS = queryMap.showHSTS;
  if (showHSTS === "true") {
    document.getElementById("advancedButton").style.display = "block";
    document.getElementById("advancedPanelAcceptButton").style.display = "none";
  }
}

/**
 * Used to display information about the SSL certificate in `error_pages.html`
 */
function toggleAdvancedAndScroll() {
  const advancedPanel = document.getElementById("badCertAdvancedPanel");
  if (advancedVisible) {
    advancedPanel.style.display = "none";
  } else {
    advancedPanel.style.display = "block";
  }
  advancedVisible = !advancedVisible;

  const horizontalLine = document.getElementById("horizontalLine");
  const advancedPanelAcceptButton = document.getElementById(
    "advancedPanelAcceptButton"
  );
  const badCertAdvancedPanel = document.getElementById("badCertAdvancedPanel");

  // We know that the button is being displayed
  if (badCertAdvancedPanel.style.display === "block") {
    horizontalLine.hidden = false;
    advancedPanelAcceptButton.scrollIntoView({
      behavior: "smooth",
      block: "center",
      inline: "nearest",
    });
  } else {
    horizontalLine.hidden = true;
  }
}

/**
 * Used to bypass an SSL pages in `error_pages.html`
 */
async function acceptAndContinue(temporary) {
  try {
    await document.addCertException(temporary);
    location.reload();
  } catch (error) {
    console.error("Unexpected error: " + error);
  }
}

document.addEventListener("DOMContentLoaded", function () {
  if (window.history.length == 1) {
    document.getElementById("advancedPanelBackButton").style.display = "none";
    document.getElementById("backFromHttp").style.display = "none";
  } else {
    document
      .getElementById("advancedPanelBackButton")
      .addEventListener("click", () => window.history.back());
    document
      .getElementById("backFromHttp")
      .addEventListener("click", () => window.history.back());
  }

  document
    .getElementById("errorTryAgain")
    .addEventListener("click", () => window.location.reload());
  document
    .getElementById("advancedButton")
    .addEventListener("click", toggleAdvancedAndScroll);
  document
    .getElementById("advancedPanelAcceptButton")
    .addEventListener("click", e => {
      const isPrivate = e.currentTarget.dataset.isPrivate;
      acceptAndContinue(!isPrivate);
    });
  document
    .getElementById("continueHttp")
    .addEventListener("click", () => document.reloadWithHttpsOnlyException());

  const query = Object.fromEntries(
    new URLSearchParams(document.documentURI.split("?")[1] || "").entries()
  );
  if (query.archiveUrl) {
    document
      .getElementById("viewArchivedButton")
      .addEventListener("click", () => {
        recordArchiveButtonClicked();
        viewArchivedVersion(query);
      });
  }
});

parseQuery(document.documentURI);
