# Implementing a Nimbus Experiment

Follow the [Android integration guide](https://experimenter.info/platform-guides/android/integration) and the [feature definition reference](https://experimenter.info/technical-reference/feature-definition). Example implementation [here](https://github.com/mozilla-mobile/fenix/pull/23996).

Nimbus FML: <https://experimenter.info/technical-reference/fml/fml-spec>

There are some clarification on how to test your Nimbus implementation:

1. After building Fenix, make sure you turn on `Secret Settings` -> `Use Nimbus Preview Collections`.
2. The experiment in <https://stage.experimenter.nonprod.webservices.mozgcp.net/nimbus/> does not have to be live for the test. In preview is sufficient.
3. Example of a test is [here](https://stage.experimenter.nonprod.webservices.mozgcp.net/nimbus/unified-search-test).
4. Make sure you archive the test after you're done with it.
5. In your PR, make sure to submit the change for .experimenter.yaml as well.
