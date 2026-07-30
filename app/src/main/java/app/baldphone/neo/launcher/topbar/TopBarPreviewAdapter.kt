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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView

import androidx.recyclerview.widget.RecyclerView

import com.bald.uriah.baldphone.R

/**
 * The chosen items, drawn in the order the bar will show them.
 *
 * It is a picture of the outcome rather than a list of settings, which is why it shows icons
 * and no words: the question it answers is "which one is second from the left", and reading
 * four labels to answer that is slower than looking.
 */
class TopBarPreviewAdapter : RecyclerView.Adapter<TopBarPreviewAdapter.SlotViewHolder>() {

    private val items = mutableListOf<TopBarItem>()

    fun submit(newItems: List<TopBarItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun current(): List<TopBarItem> = items.toList()

    /**
     * Moves one item within the strip. Called while a drag is under way, so it only touches the
     * list; saving the arrangement is the caller's business once the finger is lifted.
     */
    fun move(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices) return
        items.add(to, items.removeAt(from))
        notifyItemMoved(from, to)
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder =
        SlotViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_top_bar_preview, parent, false) as ImageView,
        )

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.icon.contentDescription = holder.icon.context.getString(item.labelRes)
    }

    class SlotViewHolder(val icon: ImageView) : RecyclerView.ViewHolder(icon)
}
