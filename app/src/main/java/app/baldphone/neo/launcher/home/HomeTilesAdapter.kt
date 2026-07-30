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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

import app.baldphone.neo.launcher.apps.AppIconBinder

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.views.FirstPageAppIcon

/**
 * Draws the home grid from a list of tiles and reports taps. Nothing else.
 *
 * What each tile *does* stays with the launcher, which knows about activities, permissions and
 * the handful of tiles whose behaviour is more than starting an intent. Keeping the adapter
 * ignorant of that is what allowed the existing actions to move across unchanged.
 */
class HomeTilesAdapter(
    private val binder: TileBinder,
) : RecyclerView.Adapter<HomeTilesAdapter.TileViewHolder>() {

    /**
     * Declared as an interface rather than a Kotlin function type because the launcher that
     * implements it is still Java, and a void method cannot satisfy a lambda returning Unit.
     */
    fun interface TileBinder {
        fun onTileBound(tile: HomeTile, view: FirstPageAppIcon)
    }

    private val tiles = mutableListOf<HomeTile>()

    /** How many rows the visible tiles are spread over, used to work out tile height. */
    var rowCount: Int = DEFAULT_ROWS
        private set

    /**
     * Height available to one row, in pixels, or zero while it is still unknown.
     *
     * This is the row's whole share of the grid, margins included; a tile gets what is left once
     * its own margins are taken out. Supplied from outside once the grid has been measured: it
     * must not be worked out during binding, because changing layout params there asks for
     * another layout pass, which binds again, and the first pass has no height to divide anyway.
     */
    var rowHeight: Int = 0
        set(value) {
            if (field == value || value <= 0) return
            field = value
            notifyItemRangeChanged(0, tiles.size)
        }

    var isEditing: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, tiles.size)
        }

    fun submit(newTiles: List<HomeTile>, spanCount: Int) {
        tiles.clear()
        tiles.addAll(newTiles)
        rowCount = maxOf(1, (newTiles.size + spanCount - 1) / spanCount)
        notifyDataSetChanged()
    }

    fun currentTiles(): List<HomeTile> = tiles.toList()

    /**
     * Moves a tile within the grid. Called while a drag is in progress, so it only touches the
     * list and tells the recycler; persisting the new order is the caller's business, once the
     * drag has finished.
     */
    fun moveTile(from: Int, to: Int) {
        if (from !in tiles.indices || to !in tiles.indices) return
        tiles.add(to, tiles.removeAt(from))
        notifyItemMoved(from, to)
    }

    override fun getItemCount(): Int = tiles.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view =
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_home_tile, parent, false) as FirstPageAppIcon
        applyHeight(view)
        return TileViewHolder(view)
    }

    /**
     * Gives a tile the height worked out from the grid, leaving the height from the layout in
     * place until that is known. Never derives it from the parent here: measuring during a
     * measure pass is what has to be avoided.
     *
     * The tile's own margins come off the row's share, or three rows of tiles would stand taller
     * than the grid by the margins between them and the whole thing would scroll.
     */
    private fun applyHeight(view: View) {
        if (rowHeight <= 0) return
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val target = rowHeight - params.topMargin - params.bottomMargin
        if (target <= 0 || params.height == target) return
        params.height = target
        view.layoutParams = params
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = tiles[position]
        val view = holder.tile

        // Height is divided rather than measured: the old rows used layout weights, and a grid
        // of naturally sized tiles would leave the reclaimed space empty again.
        applyHeight(holder.itemView)

        // This view may have been showing another tile a moment ago, and an icon load started for
        // that tile would otherwise land here after the default icon has been set.
        AppIconBinder.cancel(view.imageView)

        view.setBackgroundResource(tile.accent.backgroundRes)
        view.setText(view.context.getString(tile.labelRes))
        view.setImageResource(tile.iconRes)
        view.contentDescription = view.context.getString(tile.labelRes)

        // The launcher attaches the real behaviour, including any app the user pointed this
        // tile at. It has to run before the edit-mode override below, or it would put the
        // listener back.
        binder.onTileBound(tile, view)

        // Taps are swallowed while editing: a tile being dragged should not also launch.
        if (isEditing) {
            view.setOnClickListener(null)
            view.isClickable = false
        }
    }

    class TileViewHolder(val tile: FirstPageAppIcon) : RecyclerView.ViewHolder(tile)

    private companion object {
        const val DEFAULT_ROWS = 3
    }
}
