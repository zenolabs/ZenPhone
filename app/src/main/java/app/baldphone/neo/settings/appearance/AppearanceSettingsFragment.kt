/*
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

package app.baldphone.neo.settings.appearance

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View

import app.baldphone.neo.data.Prefs
import app.baldphone.neo.data.Theme
import app.baldphone.neo.extensions.apply
import app.baldphone.neo.permissions.PermissionManager
import app.baldphone.neo.settings.BaseSettingsFragment
import app.baldphone.neo.settings.SettingsRows
import app.baldphone.neo.ui.dialogs.BrightnessDialog

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.FontChangerActivity

/**
 * How the app looks and how big its text is.
 */
class AppearanceSettingsFragment : BaseSettingsFragment(R.layout.fragment_appearance_settings) {

    /**
     * Following the system theme only exists from Android 10, so on older devices the choice
     * is light or dark and nothing else. Built as parallel lists so the index the chooser
     * hands back maps straight onto a [Theme].
     */
    private val themes: List<Theme> =
        buildList {
            add(Theme.LIGHT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Theme.SYSTEM)
            add(Theme.DARK)
        }

    private val themeLabels: List<Int> =
        themes.map {
            when (it) {
                Theme.LIGHT -> R.string.light
                Theme.SYSTEM -> R.string.theme_system_default
                Theme.DARK -> R.string.dark
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SettingsRows.bindOption(
            row = view.findViewById(R.id.row_theme),
            titleRes = R.string.theme_settings,
            iconRes = R.drawable.ic_tabler_palette,
            optionsRes = themeLabels,
            selectedIndex = themes.indexOf(Prefs.theme).coerceAtLeast(0),
        ) { index ->
            val chosen = themes[index]
            Prefs.theme = chosen
            chosen.apply()
            requireActivity().recreate()
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_font_size),
            titleRes = R.string.font_size,
            iconRes = R.drawable.ic_tabler_text_size,
        ) {
            startActivity(Intent(requireContext(), FontChangerActivity::class.java))
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_brightness),
            titleRes = R.string.brightness,
            iconRes = R.drawable.ic_tabler_brightness,
        ) {
            // Changing screen brightness means writing a system setting, which the user has
            // to grant explicitly, so the slider is only offered once that is in hand.
            PermissionManager.checkOrRequest(requireActivity(), PermissionManager.WRITE_SETTINGS) {
                onGranted { BrightnessDialog.show(requireActivity()) }
            }
        }
    }

}
