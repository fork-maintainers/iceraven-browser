/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.pbmlock

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.privateTabs
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.lib.state.ext.flow
import mozilla.components.lib.state.ext.flowScoped
import org.mozilla.fenix.GleanMetrics.PrivateBrowsingLocked
import org.mozilla.fenix.R
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction.PrivateBrowsingLockAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.settings.biometric.BiometricPromptFeature
import org.mozilla.fenix.settings.biometric.BiometricUtils
import org.mozilla.fenix.settings.biometric.DefaultBiometricUtils

/**
 * An interface to access and observe the enabled/disabled state of the Private Browsing Lock feature.
 */
interface PrivateBrowsingLockStorage {
    /**
     * Returns the current enabled state of the private browsing lock feature.
     */
    val isFeatureEnabled: Boolean

    /**
     * Registers a listener that is invoked whenever the enabled state changes.
     *
     * @param listener A lambda that receives the new boolean value when it changes.
     */
    fun addFeatureStateListener(listener: (Boolean) -> Unit)

    /**
     * Starts observing shared preferences for changes in the feature flag.
     *
     * NB: some devices may garbage collect preference listeners very aggressively,
     * so this method should be invoked each time the feature becomes active.
     */
    fun startObservingSharedPrefs()
}

/**
 * A default implementation of `PrivateBrowsingLockStorage`.
 *
 * @param preferences The [SharedPreferences] instance from which to read the preference value.
 * @param privateBrowsingLockPrefKey The key in [SharedPreferences] representing the feature flag.
 */
class DefaultPrivateBrowsingLockStorage(
    private val preferences: SharedPreferences,
    private val privateBrowsingLockPrefKey: String,
) : PrivateBrowsingLockStorage {
    private var listener: ((Boolean) -> Unit)? = null
    private val onFeatureStateChanged = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == privateBrowsingLockPrefKey) {
            listener?.invoke(prefs.getBoolean(privateBrowsingLockPrefKey, false))
        }
    }

    override val isFeatureEnabled: Boolean
        get() = preferences.getBoolean(privateBrowsingLockPrefKey, false)

    override fun addFeatureStateListener(listener: (Boolean) -> Unit) {
        this.listener = listener
    }

    override fun startObservingSharedPrefs() {
        preferences.registerOnSharedPreferenceChangeListener(onFeatureStateChanged)
    }
}

/**
 * A lifecycle-aware feature that locks private browsing mode behind authentication
 * when certain conditions are met (e.g., switching modes or backgrounding the app).
 */
class PrivateBrowsingLockFeature(
    private val appStore: AppStore,
    private val browserStore: BrowserStore,
    private val storage: PrivateBrowsingLockStorage,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : DefaultLifecycleObserver {
    private var browserStoreScope: CoroutineScope? = null
    private var appStoreScope: CoroutineScope? = null
    private var isFeatureEnabled = false
    private var openInFirefoxRequested = false

    init {
        isFeatureEnabled = storage.isFeatureEnabled

        // Use our app state during feature init which can happen after Activity recreation.
        val isLocked =
            browserStore.state.privateTabs.isNotEmpty() && appStore.state.isPrivateScreenLocked

        updateFeatureState(
            isFeatureEnabled = isFeatureEnabled,
            isLocked = isLocked,
        )

        observeFeatureStateUpdates()
    }

    private fun observeFeatureStateUpdates() {
        storage.addFeatureStateListener { isEnabled ->
            isFeatureEnabled = isEnabled

            updateFeatureState(
                isFeatureEnabled = isFeatureEnabled,
                isLocked = appStore.state.mode == BrowsingMode.Normal &&
                        browserStore.state.privateTabs.isNotEmpty(),
            )
        }
    }

    private fun updateFeatureState(
        isFeatureEnabled: Boolean,
        isLocked: Boolean,
    ) {
        if (isFeatureEnabled) {
            activate(isLocked)
        } else {
            deactivate()
        }
    }

    private fun activate(isLocked: Boolean) {
        observePrivateTabsClosure()
        observeOpenInFirefoxRequest()

        appStore.dispatch(
            PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(
                isLocked = isLocked,
            ),
        )
    }

    private fun deactivate() {
        browserStoreScope?.cancel()
        browserStoreScope = null

        appStoreScope?.cancel()
        appStoreScope = null

        appStore.dispatch(
            PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(
                isLocked = false,
            ),
        )
    }

    private fun observePrivateTabsClosure() {
        browserStoreScope = browserStore.flowScoped(dispatcher = mainDispatcher) { flow ->
            flow
                .map { it.privateTabs.size }
                .distinctUntilChanged()
                .filter { it == 0 }
                .collect {
                    // When all private tabs are closed, we don't need to lock the private mode.
                    appStore.dispatch(
                        PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(
                            isLocked = false,
                        ),
                    )
                }
        }
    }

    // The code below is handling a specific use-case. When users want to open a private custom tab
    // in the browser via "Open in Firefox" button, while already having open private tabs in the
    // browser, we should not ask them to unlock the tab when we open it in the browser. On the
    // technical level it means that we should avoid locking private mode if there was a request to
    // open the custom tab in firefox. [AppState.openInFirefoxRequested] is the global parameter
    // that the app is using for responding to such a request. [OpenInFirefoxBinding] handles the
    // request by launching a new task and killing the custom tab, which means that by the time the
    // custom tab activity is closing, the app state has already been reset.
    // Hence, we observe the app store to record the request locally.
    private fun observeOpenInFirefoxRequest() {
        appStoreScope = appStore.flowScoped(dispatcher = mainDispatcher) { flow ->
            flow.map { it.openInFirefoxRequested }
                .distinctUntilChanged()
                .filter { it }
                .collect { openInFirefoxRequested = true }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        // We want to persist the request only within a single "user session" - between 'onStart'
        // and 'onStop' calls. 'onStart' and 'onResume' calls are significantly different within
        // this feature, because system dialogs (like permission requests) will trigger the
        // 'onPause' lifecycle event, but not the 'onStop'.
        openInFirefoxRequested = false
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)

        if (!isFeatureEnabled) return

        // Lock when the activity hits onStop unless it's a config-change restart or comes from
        // a custom tab.
        if (owner is Activity && !owner.isChangingConfigurations && !openInFirefoxRequested) {
            maybeLockPrivateModeOnStop()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        storage.startObservingSharedPrefs()
    }

    private fun maybeLockPrivateModeOnStop() {
        // When the app gets inactive with opened tabs, we lock the private mode.
        if (browserStore.state.privateTabs.isNotEmpty()) {
            appStore.dispatch(
                PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(
                    isLocked = true,
                ),
            )
        }
    }
}

/**
 * Use cases pertaining to [PrivateBrowsingLockFeature].
 *
 * @param appStore the application's [AppStore].
 */
class PrivateBrowsingLockUseCases(appStore: AppStore) {

    /**
     * Use case to be called at the end of a successful authentication.
     */
    class AuthenticatedUseCase internal constructor(private val appStore: AppStore) {
        /**
         * Handles a successful authentication event by unlocking the private browsing mode.
         *
         * This should be called by biometric or password authentication mechanisms (e.g., fingerprint,
         * face unlock, or PIN entry) once the user has successfully authenticated. It updates the app state
         * to reflect that private browsing tabs are now accessible.
         */
        operator fun invoke() {
            appStore.dispatch(
                PrivateBrowsingLockAction.UpdatePrivateBrowsingLock(isLocked = false),
            )
        }
    }

    val authenticatedUseCase by lazy { AuthenticatedUseCase(appStore) }
}

/**
 * Observes the app state and triggers a callback when the user enters a locked private browsing session.
 *
 * @param lockNormalMode If true, the callback will also be triggered in normal mode.
 * @param onPrivateModeLocked A callback invoked when private browsing mode is locked.
 */
fun Fragment.observePrivateModeLock(
    lockNormalMode: Boolean = false,
    onPrivateModeLocked: () -> Unit,
) {
    viewLifecycleOwner.run {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                observePrivateModeLock(requireComponents.appStore.flow(), lockNormalMode, onPrivateModeLocked)
            }
        }
    }
}

@VisibleForTesting
internal suspend fun observePrivateModeLock(
    flow: Flow<AppState>,
    lockNormalMode: Boolean = false,
    onPrivateModeLocked: () -> Unit,
) {
    flow
        .filter { state ->
            state.isPrivateScreenLocked && (state.mode == BrowsingMode.Private || lockNormalMode)
        }
        .distinctUntilChanged()
        .collect {
            onPrivateModeLocked()
        }
}

/**
 * Registers an [ActivityResultLauncher] that wraps handling of unlocking access to private mode
 * using the pin, pattern or password verification. This should be used in combination with
 * [verifyUser] to authenticate the user when private browsing mode is locked.
 *
 * @param onVerified an optional callback triggered on a successful authentication.
 * @return The configured [ActivityResultLauncher] to handle the pin, pattern or password verification.
 */
fun Fragment.registerForVerification(
    onVerified: (() -> Unit)? = null,
): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            handleVerificationSuccess(requireContext(), onVerified)
        } else {
            handleVerificationFailure()
        }
    }
}

/**
 * Triggers user verification to unlock access to private mode.
 *
 * Attempts biometric authentication first; falls back to launching the pin, pattern or password
 * verification. Upon success, records the telemetry and updates [AppState.isPrivateScreenLocked]
 *
 * @param biometricUtils A [BiometricPromptFeature] feature wrapper.
 * @param fallbackVerification The [ActivityResultLauncher] to handle the fallback verification.
 * @param onVerified an optional callback triggered on a successful authentication.
 */
fun Fragment.verifyUser(
    biometricUtils: BiometricUtils = DefaultBiometricUtils,
    fallbackVerification: ActivityResultLauncher<Intent>,
    onVerified: (() -> Unit)? = null,
) {
    biometricUtils.bindBiometricsCredentialsPromptOrShowWarning(
        titleRes = R.string.pbm_authentication_unlock_private_tabs,
        view = requireView(),
        onShowPinVerification = { intent -> fallbackVerification.launch(intent) },
        onAuthSuccess = { handleVerificationSuccess(requireContext(), onVerified) },
        onAuthFailure = ::handleVerificationFailure,
    )
}

private fun handleVerificationSuccess(
    context: Context,
    onVerified: (() -> Unit)? = null,
) {
    PrivateBrowsingLocked.authSuccess.record()
    context.components.useCases.privateBrowsingLockUseCases.authenticatedUseCase()

    onVerified?.invoke()
}

private fun handleVerificationFailure() {
    PrivateBrowsingLocked.authFailure.record()
}
