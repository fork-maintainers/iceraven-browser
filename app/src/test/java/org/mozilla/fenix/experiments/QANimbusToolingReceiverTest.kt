/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.experiments

import android.content.Context
import android.content.Intent
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.experiments.QANimbusToolingReceiver.Companion.ACTION_NIMBUS_TOOLING
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.nimbus.TestNimbusApi
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class QANimbusToolingReceiverTest {

    private val nimbusApi = FakeNimbusApi(testContext)
    private val receiver = QANimbusToolingReceiver(UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        every { testContext.components.nimbus.sdk } returns nimbusApi
    }

    @Test
    fun `GIVEN a tooling command WHEN the tooling action is received THEN the command is applied`() {
        receiver.onReceive(testContext, toolingIntent(ACTION_NIMBUS_TOOLING))

        assertEquals(EXPERIMENTS, nimbusApi.appliedExperiments)
        assertEquals(false, nimbusApi.fetchEnabled)
        assertTrue(nimbusApi.databaseReset)
        assertTrue(nimbusApi.stateDumped)
    }

    @Test
    fun `GIVEN a tooling command WHEN another action is received THEN the command is ignored`() {
        receiver.onReceive(testContext, toolingIntent("org.mozilla.fenix.ACTION_PRINT"))

        assertNull(nimbusApi.appliedExperiments)
        assertNull(nimbusApi.fetchEnabled)
        assertFalse(nimbusApi.databaseReset)
        assertFalse(nimbusApi.stateDumped)
    }

    @Test
    fun `GIVEN no tooling command WHEN the tooling action is received THEN nothing is applied`() {
        receiver.onReceive(testContext, Intent(ACTION_NIMBUS_TOOLING))

        assertNull(nimbusApi.appliedExperiments)
        assertNull(nimbusApi.fetchEnabled)
        assertFalse(nimbusApi.databaseReset)
        assertFalse(nimbusApi.stateDumped)
    }

    @Test
    fun `GIVEN a tooling command without the version extra WHEN the tooling action is received THEN nothing is applied`() {
        val intent = toolingIntent(ACTION_NIMBUS_TOOLING).apply { removeExtra("version") }

        receiver.onReceive(testContext, intent)

        assertNull(nimbusApi.appliedExperiments)
        assertFalse(nimbusApi.stateDumped)
    }

    private fun toolingIntent(action: String) =
        Intent(action).apply {
            putExtra("nimbus-cli", null as String?)
            putExtra("version", 1)
            putExtra("experiments", EXPERIMENTS)
            putExtra("reset-db", true)
            putExtra("log-state", true)
        }

    private class FakeNimbusApi(context: Context) : TestNimbusApi(context) {
        var appliedExperiments: String? = null
        var fetchEnabled: Boolean? = null
        var databaseReset = false
        var stateDumped = false

        override fun applyLocalExperiments(experimentsJson: String): Job {
            appliedExperiments = experimentsJson
            return completedJob()
        }

        override fun resetEnrollmentsDatabase(): Job {
            databaseReset = true
            return completedJob()
        }

        override fun setFetchEnabled(enabled: Boolean) {
            fetchEnabled = enabled
        }

        override fun dumpStateToLog() {
            stateDumped = true
        }

        private fun completedJob() = Job().apply { complete() }
    }

    companion object {
        private const val EXPERIMENTS = """{"data":[]}"""
    }
}
