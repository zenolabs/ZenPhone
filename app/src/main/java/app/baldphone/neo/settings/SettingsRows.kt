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

package app.baldphone.neo.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.google.android.material.materialswitch.MaterialSwitch

import app.baldphone.neo.extensions.setClickableAccessibilityRole
import app.baldphone.neo.ui.dialogs.BaldDialog

import com.bald.uriah.baldphone.R

/**
 * The three kinds of row the settings tree is built from.
 *
 * Every screen in the tree is a plain layout of included rows plus a handful of calls to
 * these functions. Keeping the wiring here rather than in each fragment is what makes
 * absorbing the old settings screen a matter of moving entries instead of rewriting them.
 */
object SettingsRows {

    /**
     * A row that does something when tapped: opens another screen, or hands off to Android.
     */
    fun bindAction(
        row: View,
        @StringRes titleRes: Int,
        @DrawableRes iconRes: Int,
        @StringRes subtitleRes: Int? = null,
        onClick: () -> Unit,
    ) {
        val title = row.findViewById<TextView>(R.id.title)
        title.setText(titleRes)
        row.findViewById<ImageView>(R.id.icon).setImageResource(iconRes)
        row.applySubtitle(subtitleRes?.let { row.context.getText(it) })
        row.contentDescription = title.text
        row.setOnClickListener { onClick() }
        row.setClickableAccessibilityRole()
    }

    /**
     * A row holding a yes or no answer.
     *
     * The switch is not focusable on its own: the whole row toggles it. A 96dp target is far
     * easier to hit than the switch alone for someone with a tremor, and screen readers then
     * announce a single control rather than two.
     */
    fun bindSwitch(
        row: View,
        @StringRes titleRes: Int,
        @DrawableRes iconRes: Int,
        @StringRes subtitleRes: Int? = null,
        isChecked: Boolean,
        onChange: (Boolean) -> Unit,
    ) {
        val title = row.findViewById<TextView>(R.id.title)
        val switch = row.findViewById<MaterialSwitch>(R.id.switch_widget)

        title.setText(titleRes)
        row.findViewById<ImageView>(R.id.icon).setImageResource(iconRes)
        row.applySubtitle(subtitleRes?.let { row.context.getText(it) })

        switch.isChecked = isChecked
        row.contentDescription = title.text

        row.setOnClickListener {
            val enabled = !switch.isChecked
            switch.isChecked = enabled
            onChange(enabled)
        }
        row.setClickableAccessibilityRole()
    }

    /**
     * A row holding one of several values, showing the current one underneath the title.
     */
    fun bindOption(
        row: View,
        @StringRes titleRes: Int,
        @DrawableRes iconRes: Int,
        optionsRes: List<Int>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit,
    ) {
        val context = row.context
        val title = row.findViewById<TextView>(R.id.title)
        title.setText(titleRes)
        row.findViewById<ImageView>(R.id.icon).setImageResource(iconRes)

        fun render(index: Int) {
            row.applySubtitle(optionsRes.getOrNull(index)?.let { context.getText(it) })
            row.contentDescription = "${title.text}, ${row.subtitleText()}"
        }

        var current = selectedIndex
        render(current)

        row.setOnClickListener {
            showChooser(row, titleRes, iconRes, optionsRes, current) { chosen ->
                current = chosen
                render(chosen)
                onSelected(chosen)
            }
        }
        row.setClickableAccessibilityRole()
    }

    private fun showChooser(
        row: View,
        @StringRes titleRes: Int,
        @DrawableRes iconRes: Int,
        optionsRes: List<Int>,
        selectedIndex: Int,
        onSelected: (Int) -> Unit,
    ) {
        val context = row.context
        val inflater = LayoutInflater.from(context)

        val container =
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
            }

        val dialog =
            BaldDialog
                .Builder(context)
                .setTitle(titleRes)
                .setIcon(iconRes)
                .setCustomView(container)
                .create()

        optionsRes.forEachIndexed { index, labelRes ->
            val choice = inflater.inflate(R.layout.item_option_choice, container, false)
            choice.findViewById<TextView>(R.id.label).setText(labelRes)
            // The tick keeps its space when hidden so the labels stay aligned.
            choice.findViewById<ImageView>(R.id.check).visibility =
                if (index == selectedIndex) View.VISIBLE else View.INVISIBLE
            choice.setOnClickListener {
                dialog.dismiss()
                if (index != selectedIndex) onSelected(index)
            }
            choice.setClickableAccessibilityRole()
            container.addView(choice)
        }

        dialog.show()
    }

    private fun View.applySubtitle(text: CharSequence?) {
        val subtitle = findViewById<TextView>(R.id.subtitle) ?: return
        if (text.isNullOrEmpty()) {
            subtitle.visibility = View.GONE
        } else {
            subtitle.text = text
            subtitle.visibility = View.VISIBLE
        }
    }

    private fun View.subtitleText(): CharSequence =
        findViewById<TextView>(R.id.subtitle)?.text ?: ""
}
