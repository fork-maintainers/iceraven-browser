/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.llm.ext

import mozilla.components.concept.llm.AttestationFailure
import mozilla.components.concept.llm.AuthFailure
import mozilla.components.concept.llm.AuthenticationRequired
import mozilla.components.concept.llm.Llm
import mozilla.components.lib.llm.mlpa.MlpaTokenProvider
import mozilla.components.lib.llm.mlpa.service.AuthorizationToken

/**
 * Raised when there is no signed-in account to source an FxA access token from. Tagged as
 * [AuthenticationRequired] so consumers can prompt the user to sign in.
 */
internal class FxaNotSignedIn :
    Llm.Exception("No signed-in account available for an FxA access token"), AuthenticationRequired

/**
 * Raised when an account is signed in but a fresh FxA access token could not be obtained. Tagged as
 * an [AuthFailure] so consumers surface it as an error rather than prompting the user to sign in.
 */
internal class FxaTokenUnavailable :
    Llm.Exception("Signed in but unable to obtain an FxA access token"), AuthFailure

/** The outcome of attempting to source an FxA access token. */
sealed interface FxaAccessToken {
    /** A token was obtained. */
    data class Available(val token: String) : FxaAccessToken

    /** No signed-in account is available. */
    data object NotSignedIn : FxaAccessToken

    /** An account is signed in but no token could be obtained. */
    data object Unavailable : FxaAccessToken
}

/** Convenience interface for getting an fxa access token. */
fun interface FxaAccessTokenProvider {
    /** Returns the [FxaAccessToken] outcome for the current account. */
    suspend fun provide(): FxaAccessToken
}

/**
 * Composes [tokenProviders] into one [MlpaTokenProvider].
 *
 * Providers are tried in order and the first token produced wins, so callers pass them
 * most-preferred first. If every provider fails, the failure surfaced is chosen by category via
 * [mostActionableFailure] rather than by position, so reordering or adding a provider can't
 * silently change which error the user ends up seeing.
 *
 * @param tokenProviders the [MlpaTokenProvider]s to try, most-preferred first.
 * @return an [MlpaTokenProvider].
 */
fun MlpaTokenProvider.Companion.choose(vararg tokenProviders: MlpaTokenProvider): MlpaTokenProvider {
    require(tokenProviders.isNotEmpty()) { "choose() requires at least one token provider" }
    return MlpaTokenProvider {
        val failures = mutableListOf<Throwable>()
        for (provider in tokenProviders) {
            val result = provider.fetchToken()
            if (result.isSuccess) return@MlpaTokenProvider result
            result.exceptionOrNull()?.let(failures::add)
        }
        Result.failure(failures.mostActionableFailure())
    }
}

private val FAILURE_PRECEDENCE = listOf<(Throwable) -> Boolean>(
    { it is AuthenticationRequired },
    { it is AuthFailure },
    { it is AttestationFailure },
)

private fun List<Throwable>.mostActionableFailure(): Throwable =
    FAILURE_PRECEDENCE.firstNotNullOfOrNull { matchesCategory -> firstOrNull(matchesCategory) }
        ?: last()

/** Implementation of [MlpaTokenProvider] that tries to fetch an fxa access token.
 * @param tokenProvider the [FxaAccessTokenProvider] to source a token from.
 * @return an [MlpaTokenProvider].
 */
fun MlpaTokenProvider.Companion.fxaTokenProvider(tokenProvider: FxaAccessTokenProvider) = MlpaTokenProvider {
    when (val token = tokenProvider.provide()) {
        is FxaAccessToken.Available -> Result.success(AuthorizationToken.Fxa(token.token))
        FxaAccessToken.NotSignedIn -> Result.failure(FxaNotSignedIn())
        FxaAccessToken.Unavailable -> Result.failure(FxaTokenUnavailable())
    }
}
