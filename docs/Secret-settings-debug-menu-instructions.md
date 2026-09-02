# Debug settings menu instructions

Instructions to enable the "secret settings" in the Settings menu for Firefox Nightly. This lets you access "Secret Settings" and "Secret Debug Info" in the Settings menu.

1. Tap on three-dot menu in the toolbar
2. Tap on Settings
3. Scroll down and select "About Firefox Nightly"
4. Tap the Firefox logo until you see the "Debug menu: (#) click(s) left to enable" helper message
5. Once the Debug menu has been enabled, go back to Settings and you will see "Secret Settings" and "Secret Debug Info" menu items

# Overwrite secret settings from `local.properties`

When building locally you can define a specific combination of secret settings you want to enable or disable, without toggling them manually in the app UI every time. Debug builds can force secret settings on startup from the `local.properties` file.

This only takes effect in debug builds. Other build types ignore these entries. Only boolean (on/off) secret settings are supported. Each value must be `true` or `false`.

Add one line per setting, using the `secretSettings.` prefix followed by the setting's [preference key](https://searchfox.org/firefox-main/source/mobile/android/fenix/app/src/main/res/values/preference_keys.xml):

```properties
secretSettings.pref_key_use_scroll_data_for_dynamic_toolbar=true
secretSettings.pref_key_enable_merino_client=false
```

On every rebuild of the app these values are re-applied, so any change you make manually to these particular settings in the app is reset the next time it starts.
