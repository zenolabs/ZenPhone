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

package app.baldphone.neo.launcher.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout

import com.google.android.material.materialswitch.MaterialSwitch

import app.baldphone.neo.activities.BaseActivity
import app.baldphone.neo.settings.SettingsRows
import app.baldphone.neo.ui.dialogs.showWarningSnackbar

import com.bald.uriah.baldphone.R

/**
 * Chooses which tiles the home screen carries.
 *
 * The order they sit in is settled next door, in the editor, by dragging them about; this
 * screen only answers which ones are there at all. A tile switched on joins the end of the
 * grid, where it is easy to find and easy to move.
 *
 * This replaces the old "fourth row" switch, which offered medication, apps and alarms as a
 * block of three and nothing else. There was never a reason those three belonged together
 * beyond having been drawn on the same row.
 */
class HomeTilePickerActivity : BaseActivity() {

    /**
     * The tiles now chosen, in the order they will appear.
     *
     * Held here and written on every change rather than saved on the way out: someone who
     * leaves by the home button rather than the back arrow should not lose what they did.
     */
    private val chosen = mutableListOf<HomeTile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_tile_picker)

        chosen.addAll(HomeTile.savedOrder())

        val rows = findViewById<LinearLayout>(R.id.tile_rows)
        val inflater = LayoutInflater.from(this)
        for (tile in HomeTile.entries) {
            val row = inflater.inflate(R.layout.item_setting_switch, rows, false)
            rows.addView(row)
            bindTileRow(row, tile)
        }
    }

    private fun bindTileRow(row: View, tile: HomeTile) {
        SettingsRows.bindSwitch(
            row = row,
            titleRes = tile.labelRes,
            iconRes = tile.iconRes,
            isChecked = tile in chosen,
        ) { wanted ->
            val refusal = refusalFor(wanted)
            if (refusal != null) {
                // The row has already flipped its switch by the time this runs, so a refusal
                // has to put it back, or the screen would show a state that was not saved.
                row.findViewById<MaterialSwitch>(R.id.switch_widget).isChecked = !wanted
                showWarningSnackbar(refusal)
                return@bindSwitch
            }

            if (wanted) chosen.add(tile) else chosen.remove(tile)
            HomeTile.saveOrder(chosen)
        }
    }

    /**
     * Why this change cannot be made, or null if it can.
     *
     * Both limits exist to keep the home screen usable rather than to keep it tidy. Too many
     * tiles and each one shrinks, because the grid shares out the height it has; none at all
     * and the saved order reads as "never configured", so the launcher would quietly put the
     * nine defaults back and look as though it had ignored the request.
     */
    private fun refusalFor(wanted: Boolean): Int? = when {
        wanted && chosen.size >= HomeTile.MAX_TILES -> R.string.tile_limit_reached
        !wanted && chosen.size <= 1 -> R.string.tile_last_one
        else -> null
    }
}
