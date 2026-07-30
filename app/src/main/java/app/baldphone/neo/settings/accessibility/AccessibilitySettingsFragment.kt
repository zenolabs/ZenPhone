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

package app.baldphone.neo.settings.accessibility

import android.content.Intent
import android.os.Bundle
import android.view.View

import app.baldphone.neo.data.Prefs
import app.baldphone.neo.settings.BaseSettingsFragment
import app.baldphone.neo.settings.SettingsRows

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.AccessibilityLevelChangerActivity
import com.bald.uriah.baldphone.activities.KeyboardChangerActivity

/**
 * Settings that change how the phone can be operated.
 */
class AccessibilitySettingsFragment :
    BaseSettingsFragment(R.layout.fragment_accessibility_settings) {

    /** Index 0 is left handed, index 1 right handed, matching the old dialog. */
    private val handOptions = listOf(R.string.left_handed, R.string.right_handed)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_accessibility_level),
            titleRes = R.string.accessibility_level,
            iconRes = R.drawable.ic_tabler_accessible,
        ) {
            startActivity(Intent(requireContext(), AccessibilityLevelChangerActivity::class.java))
        }

        // Left or right handed reads better as two named choices than as a switch, which
        // would have to be labelled after one hand and leave the other implied.
        SettingsRows.bindOption(
            row = view.findViewById(R.id.row_strong_hand),
            titleRes = R.string.strong_hand,
            iconRes = R.drawable.ic_tabler_hand_finger,
            optionsRes = handOptions,
            selectedIndex = if (Prefs.isRightHanded) 1 else 0,
        ) { index ->
            Prefs.isRightHanded = index == 1
            requireActivity().recreate()
        }

        SettingsRows.bindSwitch(
            row = view.findViewById(R.id.row_accidental_touches),
            titleRes = R.string.accidental_touches,
            subtitleRes = R.string.accidental_touches_settings_subtext,
            iconRes = R.drawable.ic_tabler_hand_off,
            isChecked = Prefs.useAccidentalGuard,
        ) { enabled ->
            Prefs.useAccidentalGuard = enabled
            requireActivity().recreate()
        }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_keyboard),
            titleRes = R.string.set_keyboard,
            iconRes = R.drawable.ic_tabler_keyboard,
        ) {
            startActivity(Intent(requireContext(), KeyboardChangerActivity::class.java))
        }
    }
}
