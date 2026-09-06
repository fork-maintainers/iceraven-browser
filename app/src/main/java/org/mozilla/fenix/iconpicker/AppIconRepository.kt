/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.iconpicker

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.annotation.VisibleForTesting
import org.mozilla.fenix.R

/**
 * An interface for accessing available and selected app icon options, as well as persisting the
 * selected one.
 */
interface AppIconRepository {
    /**
     * The icon selected by the user or the default one.
     */
    val selectedAppIcon: AppIcon

    /**
     * Icons available for the user to choose from.
     */
    val groupedAppIcons: Map<IconGroupTitle, List<AppIcon>>
}

/**
 * Default implementation of the [AppIconRepository]
 *
 * @param packageManager A [PackageManager] wrapper used to retrieve launcher information.
 * @param packageName The package name of the application, e.g. "org.mozilla.fenix"
 */
class DefaultAppIconRepository(
    private val packageManager: PackageManagerWrapper,
    private val packageName: String,
) : AppIconRepository {
    override val selectedAppIcon: AppIcon
        get() = AppIcon.fromString(getCurrentLauncherAliasSuffix())

    override val groupedAppIcons: Map<IconGroupTitle, List<AppIcon>>
        get() = mapOf(
            // IconGroupTitle(R.string.alternative_app_icon_group_featured) to listOf(
            //     AppIcon.AppRetro2004,
            //     AppIcon.AppPixelated,
            //     AppIcon.AppCuddling,
            //     AppIcon.AppPride,
            //     AppIcon.AppFlaming,
            //     AppIcon.AppMinimal,
            //     AppIcon.AppMomo,
            //     AppIcon.AppCool,
            // ),
            IconGroupTitle(R.string.alternative_app_icon_group_solid_colors) to listOf(
                AppIcon.AppDefault,
                AppIcon.AppSolidLight,
                AppIcon.AppSolidDark,
                AppIcon.AppSolidRed,
                AppIcon.AppSolidGreen,
                AppIcon.AppSolidBlue,
                AppIcon.AppSolidPurple,
                AppIcon.AppSolidPurpleDark,
            ),
            IconGroupTitle(R.string.alternative_app_icon_group_gradients) to listOf(
                AppIcon.AppGradientSunrise,
                AppIcon.AppGradientGoldenHour,
                AppIcon.AppGradientSunset,
                AppIcon.AppGradientBlueHour,
                AppIcon.AppGradientTwilight,
                AppIcon.AppGradientMidnight,
                AppIcon.AppGradientNorthernLights,
            ),
        )

    @VisibleForTesting
    internal fun getCurrentLauncherAliasSuffix(): String {
        return packageManager.getFenixLauncherName(packageName)
            ?.substringAfter("$packageName.")
            // In a very unlikely case that there is no active aliases,
            // we will show the default icon as selected.
            ?: AppIcon.AppDefault.aliasSuffix
    }
}

/**
 * A wrapper interface for [PackageManager], allowing edge case testing when getting launcher info.
 */
interface PackageManagerWrapper {

    /**
     * Retrieves the [ResolveInfo] for the given package if there is one registered, and returns its
     * activity name.
     *
     * @param packageName The package name of the application, e.g. "org.mozilla.fenix"
     */
    fun getFenixLauncherName(packageName: String): String?
}

/**
 * A default implementation of [PackageManagerWrapper].
 */
class DefaultPackageManagerWrapper(val packageManager: PackageManager) : PackageManagerWrapper {
    override fun getFenixLauncherName(packageName: String): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }

        return packageManager.queryIntentActivities(intent, 0)
            // Extra guard to filter out external intent activities that might be bound
            // to our package, e.g. "leakcanary.internal.activity.LeakLauncherActivity"
            .firstOrNull { resolveInfo -> resolveInfo.activityInfo.name.startsWith(packageName) }
            ?.activityInfo
            ?.name
    }
}
