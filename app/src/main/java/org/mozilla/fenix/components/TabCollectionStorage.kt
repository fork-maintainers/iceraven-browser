/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import android.content.Context
import android.os.StrictMode
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.browser.state.state.createTab
import mozilla.components.feature.tab.collections.Tab
import mozilla.components.feature.tab.collections.TabCollection
import mozilla.components.feature.tab.collections.TabCollectionStorage
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import mozilla.components.support.ktx.kotlin.toShortUrl
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.perf.StrictModeManager

private const val COLLECTION_MAX_TITLE_LENGTH = 20

class TabCollectionStorage(
    private val context: Context,
    strictMode: StrictModeManager,
    private val delegate: Observable<Observer> = ObserverRegistry(),
) : Observable<org.mozilla.fenix.components.TabCollectionStorage.Observer> by delegate {

    /** Interface to be implemented by classes that want to observe the storage */
    interface Observer {
        /** A collection has been created */
        fun onCollectionCreated(title: String, sessions: List<TabSessionState>, id: Long?) = Unit

        /** Tab(s) have been added to collection */
        fun onTabsAdded(tabCollection: TabCollection, sessions: List<TabSessionState>) = Unit

        /** Collection has been renamed */
        fun onCollectionRenamed(tabCollection: TabCollection, title: String) = Unit
    }

    var cachedTabCollections = listOf<TabCollection>()

    private val collectionStorage by lazy {
        strictMode.allowViolation(StrictMode::allowThreadDiskReads) {
            TabCollectionStorage(context)
        }
    }

    suspend fun createCollection(title: String, sessions: List<TabSessionState>): Long? {
        val id = collectionStorage.createCollection(title, sessions)
        notifyObservers { onCollectionCreated(title, sessions, id) }
        return id
    }

    suspend fun createCollection(tabCollection: TabCollection): Long? {
        val sessions = tabCollection.tabs.map { createTab(url = it.url, title = it.title) }
        val id = collectionStorage.createCollection(tabCollection.title, sessions)
        notifyObservers { onCollectionCreated(tabCollection.title, sessions, id) }
        return id
    }

    suspend fun addTabsToCollection(tabCollection: TabCollection, sessions: List<TabSessionState>): Long? {
        val id = collectionStorage.addTabsToCollection(tabCollection, sessions)
        notifyObservers { onTabsAdded(tabCollection, sessions) }
        return id
    }

    fun getCollections(): LiveData<List<TabCollection>> {
        return collectionStorage.getCollections().asLiveData()
    }

    /** Returns the list of stored [TabCollection]s. */
    suspend fun getCollectionsList(): List<TabCollection> = collectionStorage.getCollectionsList()

    suspend fun removeCollection(tabCollection: TabCollection) {
        collectionStorage.removeCollection(tabCollection)
    }

    suspend fun removeTabFromCollection(tabCollection: TabCollection, tab: Tab) {
        collectionStorage.removeTabFromCollection(tabCollection, tab)
    }

    suspend fun renameCollection(tabCollection: TabCollection, title: String) {
        collectionStorage.renameCollection(tabCollection, title)
        notifyObservers { onCollectionRenamed(tabCollection, title) }
    }
}

fun TabCollection.description(context: Context): String {
    return this.tabs
        .map { it.url.toShortUrl(context.components.publicSuffixList) }
        .map {
            if (it.length > COLLECTION_MAX_TITLE_LENGTH) {
                it.substring(
                    0,
                    COLLECTION_MAX_TITLE_LENGTH,
                ) + "…"
            } else {
                it
            }
        }
        .distinct()
        .joinToString(", ")
}
