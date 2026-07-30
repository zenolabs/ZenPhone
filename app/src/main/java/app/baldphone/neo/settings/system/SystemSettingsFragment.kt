/*
 * Copyright 2025 Damian Kuzmiak
 * Copyright 2026 Zenolabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.baldphone.neo.settings.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.nfc.NfcManager
import android.os.Bundle
import android.provider.Settings
import android.view.View

import androidx.navigation.fragment.findNavController

import app.baldphone.neo.settings.BaseSettingsFragment
import app.baldphone.neo.settings.SettingsRows

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.FakeLauncherActivity
import com.bald.uriah.baldphone.utils.BaldToast

/**
 * The phone itself: permissions, which launcher is in charge, language, and shortcuts into
 * Android's own connectivity screens.
 */
class SystemSettingsFragment : BaseSettingsFragment(R.layout.fragment_system_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SettingsRows.bindAction(
            row = view.findViewById(R.id.btn_permissions),
            titleRes = R.string.permissions_part,
            iconRes = R.drawable.grant_all_permissions_on_button,
        ) {
            findNavController().navigate(R.id.action_system_to_permissions)
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_default_launcher),
            titleRes = R.string.set_home_screen,
            iconRes = R.drawable.ic_tabler_home,
        ) {
            FakeLauncherActivity.resetPreferredLauncherAndOpenChooser(requireContext())
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_language),
            titleRes = R.string.language_settings,
            iconRes = R.drawable.ic_tabler_language,
        ) { openSystemSettings(Settings.ACTION_LOCALE_SETTINGS) }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_wifi),
            titleRes = R.string.wifi,
            iconRes = R.drawable.ic_tabler_wifi,
        ) {
            openSystemSettings(
                Settings.ACTION_WIFI_SETTINGS,
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT,
            )
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_bluetooth),
            titleRes = R.string.bluetooth,
            iconRes = R.drawable.ic_tabler_bluetooth,
        ) { openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_airplane),
            titleRes = R.string.airplane_mode,
            iconRes = R.drawable.ic_tabler_plane,
        ) { openSystemSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }

        bindNfcRow(view)

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_location),
            titleRes = R.string.location,
            iconRes = R.drawable.ic_tabler_map_pin,
        ) { openSystemSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS) }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_android_settings),
            titleRes = R.string.advanced_options,
            iconRes = R.drawable.ic_tabler_settings,
        ) { openSystemSettings(Settings.ACTION_SETTINGS) }
    }

    /**
     * Hidden outright on hardware without an NFC chip: a row that can only ever report a
     * missing feature is worse than no row at all.
     */
    private fun bindNfcRow(view: View) {
        val row = view.findViewById<View>(R.id.row_nfc)
        val manager = requireContext().getSystemService(Context.NFC_SERVICE) as? NfcManager

        if (manager?.defaultAdapter == null) {
            row.visibility = View.GONE
            return
        }

        SettingsRows.bindAction(
            row = row,
            titleRes = R.string.nfc,
            iconRes = R.drawable.ic_tabler_nfc,
        ) { openSystemSettings(Settings.ACTION_NFC_SETTINGS) }
    }

    private fun openSystemSettings(action: String, flags: Int = 0) {
        try {
            startActivity(Intent(action).apply { if (flags != 0) addFlags(flags) })
        } catch (e: ActivityNotFoundException) {
            BaldToast
                .from(requireContext())
                .setText(R.string.setting_does_not_exist)
                .setType(BaldToast.TYPE_ERROR)
                .show()
        }
    }
}
