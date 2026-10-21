/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.listentopage

import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import java.util.concurrent.Executor

/** How hot the device is, from coolest to hottest, or [Unknown]. */
enum class ThermalStatus {
    None,
    Light,
    Moderate,
    Severe,
    Critical,
    Emergency,
    Shutdown,
    Unknown,
}

/** Reports the thermal status of the device. */
interface ThermalStatusMonitor {
    /** The status now. */
    val currentStatus: ThermalStatus

    /**
     * Whether the device reports a status that can be trusted. Devices with less advanced thermal HALs should report
     * UNKNOWN forever.
     */
    fun isSupported(): Boolean

    /** Start observing hardware sensors. */
    fun startObserving(onStatusChanged: (status: ThermalStatus) -> Unit)

    /** Stop observing hardware sensors. */
    fun stopObserving()
}

/** [ThermalStatusMonitor] backed by [PowerManager]. */
@RequiresApi(Build.VERSION_CODES.R)
class PowerManagerThermalStatusMonitor(
    private val powerManager: PowerManager,
    private val executor: Executor,
) : ThermalStatusMonitor {

    private var listener: PowerManager.OnThermalStatusChangedListener? = null

    override val currentStatus: ThermalStatus
        get() = powerManager.currentThermalStatus.toThermalStatus()

    private var isKnownSupported = false

    // The headroom is NaN for thermal HALs which cannot forecast skin temperature, requiring at least HAL 2.0. It is
    // also NaN when asked for more than about once a second, so a device that has shown support is not asked again.
    override fun isSupported(): Boolean {
        if (!isKnownSupported) isKnownSupported = !powerManager.getThermalHeadroom(0).isNaN()
        return isKnownSupported
    }

    override fun startObserving(onStatusChanged: (status: ThermalStatus) -> Unit) {
        stopObserving()
        PowerManager.OnThermalStatusChangedListener { status -> onStatusChanged(status.toThermalStatus()) }
            .also { powerManager.addThermalStatusListener(executor, it) }
            .also { listener = it }
    }

    override fun stopObserving() {
        listener?.let { powerManager.removeThermalStatusListener(it) }
        listener = null
    }
}

private fun Int.toThermalStatus(): ThermalStatus =
    when (this) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.None
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.Light
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.Moderate
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.Severe
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.Critical
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.Emergency
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.Shutdown
        else -> ThermalStatus.Unknown
    }
