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

package app.baldphone.neo.launcher.topbar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.android.material.materialswitch.MaterialSwitch

import app.baldphone.neo.activities.BaseActivity
import app.baldphone.neo.settings.SettingsRows
import app.baldphone.neo.ui.dialogs.showWarningSnackbar

import com.bald.uriah.baldphone.R

/**
 * Chooses which four indicators the strip along the top of the home screen carries.
 *
 * Deliberately the same screen, in shape and in behaviour, as the one that chooses the home
 * tiles: a switch to a line, a refusal when there is no room, and a refusal to leave nothing.
 * Someone who has met one of these two screens has met both.
 */
class TopBarPickerActivity : BaseActivity() {

    /**
     * What is chosen, in the order it will appear.
     *
     * Written on every change rather than on the way out, so leaving by the home button rather
     * than the back arrow loses nothing.
     */
    private val chosen = mutableListOf<TopBarItem>()

    private val preview = TopBarPreviewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_top_bar_picker)

        chosen.addAll(TopBarItem.savedOrder())

        setUpPreview()

        val rows = findViewById<LinearLayout>(R.id.top_bar_rows)
        val inflater = LayoutInflater.from(this)
        for (item in TopBarItem.entries) {
            val row = inflater.inflate(R.layout.item_setting_switch, rows, false)
            rows.addView(row)
            bindItemRow(row, item)
        }
    }

    private fun setUpPreview() {
        val strip = findViewById<RecyclerView>(R.id.top_bar_preview)
        (strip.layoutManager as LinearLayoutManager).orientation = LinearLayoutManager.HORIZONTAL
        strip.adapter = preview
        preview.submit(chosen)

        // Taken once the strip has been measured and again whenever its size changes, since the
        // share each slot gets depends on how many there are and that changes as switches are
        // turned on and off.
        strip.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateSlotWidth(strip) }

        // Sideways only: the strip is one row and there is nowhere above or below to go.
        ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                0,
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean {
                    val from = viewHolder.bindingAdapterPosition
                    val to = target.bindingAdapterPosition
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                        return false
                    }
                    preview.move(from, to)
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                ) {
                    super.clearView(recyclerView, viewHolder)
                    // Written once the icon is let go, not at every swap along the way.
                    chosen.clear()
                    chosen.addAll(preview.current())
                    TopBarItem.saveOrder(chosen)
                }
            },
        ).attachToRecyclerView(strip)
    }

    private fun bindItemRow(row: View, item: TopBarItem) {
        SettingsRows.bindSwitch(
            row = row,
            titleRes = item.labelRes,
            iconRes = item.iconRes,
            subtitleRes = item.descriptionRes,
            isChecked = item in chosen,
        ) { wanted ->
            val refusal = refusalFor(wanted)
            if (refusal != null) {
                // The row has already flipped its switch by the time this runs, so a refusal
                // has to put it back or the screen would show a state that was never saved.
                row.findViewById<MaterialSwitch>(R.id.switch_widget).isChecked = !wanted
                showWarningSnackbar(refusal)
                return@bindSwitch
            }

            // Added at the end, where a new thing is looked for, and moved from there by
            // dragging it in the strip above.
            if (wanted) chosen.add(item) else chosen.remove(item)
            TopBarItem.saveOrder(chosen)
            preview.submit(chosen)
            updateSlotWidth(findViewById(R.id.top_bar_preview))
        }
    }

    /**
     * Divides the strip between however many icons are in it.
     *
     * Always posted, never applied on the spot: this is reached from a layout callback, and
     * telling a RecyclerView its items changed while it is laying out throws.
     */
    private fun updateSlotWidth(strip: RecyclerView) {
        val available = strip.width - strip.paddingStart - strip.paddingEnd
        val count = chosen.size
        if (available <= 0 || count <= 0) return
        strip.post { preview.slotWidth = available / count }
    }

    /**
     * Why this change cannot be made, or null if it can.
     *
     * The upper limit is about the size of what is left: four already divide the width of the
     * screen between them, and a fifth does not add a button so much as shrink four. The lower
     * one is about honesty - an empty choice reads as "never configured", so the bar would
     * quietly fill itself back up and look as though the request had been ignored.
     */
    private fun refusalFor(wanted: Boolean): Int? = when {
        wanted && chosen.size >= TopBarItem.MAX_ITEMS -> R.string.top_bar_limit_reached
        !wanted && chosen.size <= 1 -> R.string.top_bar_last_one
        else -> null
    }
}
