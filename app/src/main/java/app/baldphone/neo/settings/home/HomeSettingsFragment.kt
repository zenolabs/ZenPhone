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

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.google.android.material.materialswitch.MaterialSwitch

import app.baldphone.neo.data.Prefs
import app.baldphone.neo.settings.BaseSettingsFragment

import com.bald.uriah.baldphone.R

/**
 * Settings for the home screen itself.
 *
 * This is the first screen in the new settings tree to carry a toggle rather than a link,
 * so [bindSwitchRow] is written to be reused by whatever comes next.
 */
class HomeSettingsFragment : BaseSettingsFragment(R.layout.fragment_home_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindSwitchRow(
            row = view.findViewById(R.id.row_fourth_row),
            titleRes = R.string.settings_fourth_row,
            subtitleRes = R.string.settings_fourth_row_subtext,
            iconRes = R.drawable.ic_lucide_layout_grid,
            isChecked = Prefs.isFourthHomeRowEnabled,
        ) { enabled -> Prefs.isFourthHomeRowEnabled = enabled }
    }

    private fun bindSwitchRow(
        row: View,
        titleRes: Int,
        subtitleRes: Int?,
        iconRes: Int,
        isChecked: Boolean,
        onChange: (Boolean) -> Unit,
    ) {
        val title = row.findViewById<TextView>(R.id.title)
        val subtitle = row.findViewById<TextView>(R.id.subtitle)
        val switch = row.findViewById<MaterialSwitch>(R.id.switch_widget)

        title.setText(titleRes)
        row.findViewById<ImageView>(R.id.icon).setImageResource(iconRes)

        if (subtitleRes != null) {
            subtitle.setText(subtitleRes)
            subtitle.visibility = View.VISIBLE
        } else {
            subtitle.visibility = View.GONE
        }

        switch.isChecked = isChecked
        row.contentDescription = title.text

        // The row owns the interaction; the switch is only a visual state indicator.
        row.setOnClickListener {
            val enabled = !switch.isChecked
            switch.isChecked = enabled
            onChange(enabled)
        }
    }
}
