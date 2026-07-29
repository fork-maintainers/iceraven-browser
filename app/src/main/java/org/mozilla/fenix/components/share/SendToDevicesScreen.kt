/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.BottomSheetHandle
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.DeviceType
import org.mozilla.fenix.R
import org.mozilla.fenix.components.menu.compose.MenuGroup
import org.mozilla.fenix.components.menu.compose.MenuItem
import org.mozilla.fenix.share.ShareViewModel
import org.mozilla.fenix.share.listadapters.SyncShareOption
import org.mozilla.fenix.theme.FirefoxTheme
import mozilla.components.ui.icons.R as IconsR

private val NoDevicesAvailableDescriptionHorizontalPadding = 16.dp

@Composable
internal fun SendToDevicesContent(
    uiState: ShareViewModel.ShareUiState,
    onDismiss: () -> Unit,
    onSendToDevice: (SyncShareOption.SingleDevice) -> Unit,
    onSendToAll: () -> Unit,
) {
    if (uiState.isLoading) return

    val singleDevices = uiState.devices.filterIsInstance<SyncShareOption.SingleDevice>()
    FirefoxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            BottomSheetHandle(
                onRequestDismiss = onDismiss,
                contentDescription = stringResource(
                    R.string.send_to_devices_bottom_sheet_close_content_description,
                ),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally),
            )
            Text(
                text = stringResource(id = R.string.share_device_subheader),
                style = FirefoxTheme.typography.headline7,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = 16.dp,
                        top = 8.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
            )
            if (singleDevices.isEmpty()) {
                NoDevicesAvailable()
            } else {
                DeviceList(
                    devices = singleDevices,
                    onDeviceClick = onSendToDevice,
                )
            }
            if (singleDevices.size > 1) {
                Spacer(modifier = Modifier.size(8.dp))
                SendToAllItem(onSendToAll = onSendToAll)
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<SyncShareOption.SingleDevice>,
    onDeviceClick: (SyncShareOption.SingleDevice) -> Unit,
) {
    MenuGroup {
        for (option in devices) {
            MenuItem(
                label = option.device.displayName,
                beforeIconPainter = painterResource(
                    id = if (option.device.deviceType == DeviceType.MOBILE) {
                        IconsR.drawable.mozac_ic_device_mobile_24
                    } else {
                        IconsR.drawable.mozac_ic_device_desktop_24
                    },
                ),
                onClick = { onDeviceClick(option) },
            )
        }
    }
}

@Composable
private fun SendToAllItem(onSendToAll: () -> Unit) {
    MenuGroup {
        MenuItem(
            label = stringResource(id = R.string.sync_send_to_all),
            beforeIconPainter = painterResource(id = IconsR.drawable.mozac_ic_select_all_24),
            onClick = onSendToAll,
        )
    }
}

@Composable
private fun ColumnScope.NoDevicesAvailable() {
    Image(
        painter = painterResource(id = R.drawable.send_link_to_device_no_devices_available),
        contentDescription = stringResource(id = R.string.synced_tabs_connect_another_device),
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.CenterHorizontally),
    )

    Text(
        text = stringResource(id = R.string.sync_no_devices_available),
        style = FirefoxTheme.typography.headline6,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    )

    Text(
        text = stringResource(R.string.sync_no_devices_available_description),
        style = FirefoxTheme.typography.body2,
        color = MaterialTheme.colorScheme.secondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = NoDevicesAvailableDescriptionHorizontalPadding),
    )

    Spacer(modifier = Modifier.height(40.dp))
}

private fun previewDevice(name: String, type: DeviceType) = SyncShareOption.SingleDevice(
    device = Device(
        id = name,
        displayName = name,
        deviceType = type,
        isCurrentDevice = false,
        lastAccessTime = null,
        capabilities = emptyList(),
        subscriptionExpired = false,
        subscription = null,
    ),
)

@PreviewLightDark
@Composable
private fun SendToDevicesContentWithDevicesPreview() {
    FirefoxTheme {
        Surface {
            SendToDevicesContent(
                uiState = ShareViewModel.ShareUiState(
                    devices = listOf(
                        previewDevice("My Phone", DeviceType.MOBILE),
                        previewDevice("My Laptop", DeviceType.DESKTOP),
                    ),
                ),
                onDismiss = {},
                onSendToDevice = {},
                onSendToAll = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SendToDevicesContentNoDevicesPreview() {
    FirefoxTheme {
        Surface {
            SendToDevicesContent(
                uiState = ShareViewModel.ShareUiState(devices = emptyList()),
                onDismiss = {},
                onSendToDevice = {},
                onSendToAll = {},
            )
        }
    }
}
