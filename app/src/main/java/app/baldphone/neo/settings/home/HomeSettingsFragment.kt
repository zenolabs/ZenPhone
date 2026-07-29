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

package app.baldphone.neo.settings.home

import android.content.Intent
import android.os.Bundle
import android.view.View

import app.baldphone.neo.data.Prefs
import app.baldphone.neo.data.StatusBarMode
import app.baldphone.neo.settings.BaseSettingsFragment
import app.baldphone.neo.settings.SettingsRows

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.Page1EditorActivity
import com.bald.uriah.baldphone.activities.pills.PillTimeSetterActivity

/**
 * Settings for the home screen itself.
 */
class HomeSettingsFragment : BaseSettingsFragment(R.layout.fragment_home_settings) {

    /** Kept in the order Android reports them, so the index maps straight onto the enum. */
    private val statusBarOptions =
        listOf(R.string.nowhere, R.string.only_home_screen, R.string.everywhere)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SettingsRows.bindSwitch(
            row = view.findViewById(R.id.row_fourth_row),
            titleRes = R.string.settings_fourth_row,
            subtitleRes = R.string.settings_fourth_row_subtext,
            iconRes = R.drawable.ic_tabler_layout_grid,
            isChecked = Prefs.isFourthHomeRowEnabled,
        ) { enabled -> Prefs.isFourthHomeRowEnabled = enabled }


        SettingsRows.bindOption(
            row = view.findViewById(R.id.row_status_bar),
            titleRes = R.string.status_bar_settings,
            iconRes = R.drawable.ic_tabler_layout_navbar,
            optionsRes = statusBarOptions,
            selectedIndex = Prefs.statusBarMode.value,
        ) { index ->
            Prefs.statusBarMode = StatusBarMode.fromValue(index)
            requireActivity().recreate()
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_edit_home),
            titleRes = R.string.edit_home_screen,
            iconRes = R.drawable.ic_tabler_pencil,
        ) {
            startActivity(Intent(requireContext(), Page1EditorActivity::class.java))
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_pill_times),
            titleRes = R.string.time_changer,
            iconRes = R.drawable.ic_tabler_pill,
        ) {
            startActivity(Intent(requireContext(), PillTimeSetterActivity::class.java))
        }
    }
}
