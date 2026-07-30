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

package app.baldphone.neo.ui.dialogs

import android.app.Activity
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.SeekBar

import com.bald.uriah.baldphone.R

/**
 * The screen brightness slider, with the automatic setting beside it.
 *
 * Lifted out of the appearance settings so the top bar can offer it too. Whoever calls this
 * must already hold WRITE_SETTINGS: brightness is a system setting, and the dialog would come
 * up looking usable and then quietly fail to save anything.
 */
object BrightnessDialog {

    /**
     * Never all the way down. A screen dimmed to nothing cannot be brought back by someone who
     * can no longer see the slider they have just dragged.
     */
    private const val MINIMUM_BRIGHTNESS = 20

    fun show(activity: Activity) {
        val resolver = activity.contentResolver
        val holder =
            LayoutInflater.from(activity).inflate(R.layout.brightness_seek_bar, null, false)
        val seekBar = holder.findViewById<SeekBar>(R.id.brightness_seek_bar)
        val autoCheckBox = holder.findViewById<CheckBox>(R.id.auto_brightness_check_box)

        seekBar.keyProgressIncrement = 1

        val isAutomatic =
            runCatching {
                Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            }.getOrDefault(false)

        if (isAutomatic) {
            autoCheckBox.isChecked = true
            seekBar.isEnabled = false
        } else {
            runCatching { Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS) }
                .onSuccess { seekBar.progress = it }
        }

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    applyToWindow(activity, progress.coerceAtLeast(MINIMUM_BRIGHTNESS))
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit

                override fun onStopTrackingTouch(bar: SeekBar) {
                    val value = bar.progress.coerceAtLeast(MINIMUM_BRIGHTNESS)
                    Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, value)
                    applyToWindow(activity, value)
                }
            },
        )

        autoCheckBox.setOnCheckedChangeListener { _, isChecked ->
            seekBar.isEnabled = !isChecked
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (isChecked) {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                } else {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                },
            )
        }

        BaldDialog
            .Builder(activity)
            .setTitle(R.string.brightness)
            .setMessage(R.string.brightness_subtext)
            .setIcon(R.drawable.ic_tabler_brightness)
            .setCustomView(holder)
            .setPositiveButton(activity.getText(R.string.ok))
            .show()
    }

    /** Shows the change at once, before it is written, so dragging the slider is not guesswork. */
    private fun applyToWindow(activity: Activity, brightness: Int) {
        activity.window.attributes =
            activity.window.attributes.apply {
                screenBrightness = brightness / 255f
            }
    }
}
