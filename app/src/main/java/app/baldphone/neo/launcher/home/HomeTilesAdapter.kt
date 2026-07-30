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
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

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
    private val onTileClick: (HomeTile) -> Unit,
    private val onTileBound: (HomeTile, FirstPageAppIcon) -> Unit,
) : RecyclerView.Adapter<HomeTilesAdapter.TileViewHolder>() {

    private val tiles = mutableListOf<HomeTile>()

    /** How many rows the visible tiles are spread over, used to work out tile height. */
    var rowCount: Int = DEFAULT_ROWS
        private set

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
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        val tile = tiles[position]
        val view = holder.tile

        // Height is divided rather than measured: the old rows used layout weights, and a grid
        // of naturally sized tiles would leave the reclaimed space empty again.
        holder.itemView.layoutParams =
            holder.itemView.layoutParams.apply {
                val available = (holder.itemView.parent as? ViewGroup)?.height ?: 0
                if (available > 0) height = available / rowCount
            }

        view.setBackgroundResource(tile.accent.backgroundRes)
        view.setText(view.context.getString(tile.labelRes))
        view.setImageResource(tile.iconRes)
        view.contentDescription = view.context.getString(tile.labelRes)

        // Taps are swallowed while editing: a tile being dragged should not also launch.
        view.setOnClickListener(if (isEditing) null else { _ -> onTileClick(tile) })
        view.isClickable = !isEditing

        onTileBound(tile, view)
    }

    class TileViewHolder(val tile: FirstPageAppIcon) : RecyclerView.ViewHolder(tile)

    private companion object {
        const val DEFAULT_ROWS = 3
    }
}
