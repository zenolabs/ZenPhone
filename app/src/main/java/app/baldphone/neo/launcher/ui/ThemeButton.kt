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

package app.baldphone.neo.launcher.ui

import android.content.Context
import android.util.AttributeSet

import androidx.appcompat.widget.AppCompatImageButton

import app.baldphone.neo.data.Prefs
import app.baldphone.neo.data.Theme
import app.baldphone.neo.extensions.apply
import app.baldphone.neo.extensions.isDarkTheme

import com.bald.uriah.baldphone.R

/**
 * Switches the phone between its light and dark appearance.
 *
 * The icon shows how things are now - a sun for the light appearance, a moon for the dark -
 * rather than what a touch would bring. That is what the buttons beside it do: Wi-Fi, sound
 * and the torch all report the state of things, and an icon here that pointed at its
 * destination instead would be the only one in the row speaking differently.
 *
 * Touching it settles on a colour rather than a rule. Someone whose theme follows the system
 * and who taps this has said they want it light, or dark, now - so the preference stops
 * following the system and says which. Losing the rule is the point of having asked.
 */
class ThemeButton
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.imageButtonStyle,
    ) : AppCompatImageButton(context, attrs, defStyleAttr) {

        init {
            setOnClickListener { toggle() }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            render()
        }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            // The theme can be changed in the settings while this sits behind them, and that
            // does not always take the whole activity down with it.
            if (visibility == VISIBLE) render()
        }

        private fun toggle() {
            // Read from how it actually looks, not from the preference. With the preference on
            // "follow the system" the two disagree, and what the person is answering is what is
            // in front of them.
            val wanted = if (context.isDarkTheme) Theme.LIGHT else Theme.DARK
            Prefs.theme = wanted
            wanted.apply()
            render()
        }

        private fun render() {
            val dark = context.isDarkTheme
            setImageResource(if (dark) R.drawable.ic_tabler_moon else R.drawable.ic_tabler_sun)
            contentDescription =
                context.getString(
                    if (dark) R.string.theme_dark_accessibility else R.string.theme_light_accessibility,
                )
        }
    }
