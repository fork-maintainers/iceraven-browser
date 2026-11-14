/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.listentopage

import mozilla.components.concept.base.profiler.Profiler

/** A marker as [FakeProfiler] was given it. */
internal data class Marker(val name: String, val startTime: Double?, val endTime: Double?, val text: String?)

/** A [Profiler] whose time is [clock], recording the markers it is given while [active]. */
internal class FakeProfiler(private val clock: () -> Long) : Profiler {
    var active = true
    val markers = mutableListOf<Marker>()

    override fun isProfilerActive() = active

    override fun getProfilerTime() = clock().toDouble().takeIf { active }

    override fun addMarker(markerName: String, startTime: Double?, endTime: Double?, text: String?) {
        markers += Marker(markerName, startTime, endTime, text)
    }

    override fun addMarker(markerName: String, startTime: Double?, text: String?) = Unit

    override fun addMarker(markerName: String, startTime: Double?) = Unit

    override fun addMarker(markerName: String, text: String?) = Unit

    override fun addMarker(markerName: String) = Unit

    override fun startProfiler(filters: Array<String>, features: Array<String>) = Unit

    override fun stopProfiler(onSuccess: (ByteArray?) -> Unit, onError: (Throwable) -> Unit) = Unit
}
