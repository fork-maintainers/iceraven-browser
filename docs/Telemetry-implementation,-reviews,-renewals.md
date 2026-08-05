# Telemetry - Implementation, Reviews, Renewals

## Creating Glean Annotations

Glean Annotations repository: <https://github.com/mozilla/glean-annotations>

See the documentation on how to [create new annotations](https://mozilla.github.io/glean-annotations/contributing/creating/).

## Data review

Data reviews are needed on all patches that add new telemetry or modify existing telemetry.
Any change that touches metrics will be automatically flagged with a `needs-data-classification` tag by Phabricator.
If a change adds/updates data collection in a way that doesn’t automatically trigger this rule, this tag should be added manually (and if appropriate, please file a bug to update the herald rule so it happens automatically next time).

More details about this process can be found in the [in-tree docs](https://firefox-source-docs.mozilla.org/contributing/data-review.html) and [wiki](https://wiki.mozilla.org/Data_Collection).
Add a link for the bug adding or changing a metric or ping to the `bugs` and `data_reviews` lists

Example:

```
download_notification:
  resume:
    type: event
    description: |
      A user resumed a download in the download notification
    bugs:
      - https://bugzilla.mozilla.org/show_bug.cgi?id=EXAMPLE
    data_reviews:
      - https://bugzilla.mozilla.org/show_bug.cgi?id=EXAMPLE
    data_sensitivity:
      - interaction
    notification_emails:
      - fenix-core@mozilla.com
    expires: "2021-07-01"
```

When a telemetry metric is being renewed, do not remove the old data review links from `metrics.yaml`. The new approval should be added to the existing list.

Make sure you are selecting the correct category of data that is being collected: [Data Collection Categories](https://wiki.mozilla.org/Data_Collection#Data_Collection_Categories)
