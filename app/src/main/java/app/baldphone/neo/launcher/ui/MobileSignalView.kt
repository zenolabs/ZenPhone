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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.AttributeSet

import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

import com.bald.uriah.baldphone.R

/**
 * How much reception there is, in the five steps the platform reports.
 *
 * The steps are not a choice: `getLevel` answers 0 to 4 and nothing finer, so the icons come
 * in the same five and nothing is interpolated between them.
 *
 * When the permission is refused it shows an empty outline, dimmed, and says as much to a
 * screen reader - never the crossed-out icon. Crossed out means "no reception here", which
 * would be a lie told to someone who might then walk to a window looking for a signal they
 * already had. A view that cannot answer should look like it cannot answer.
 */
class MobileSignalView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.imageButtonStyle,
    ) : AppCompatImageButton(context, attrs, defStyleAttr) {

        private var listening = false

        /** Registered on newer Android, where the older listener is deprecated and inert. */
        private val callback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                object :
                    TelephonyCallback(),
                    TelephonyCallback.SignalStrengthsListener {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        render(signalStrength.level)
                    }
                }
            } else {
                null
            }

        @Suppress("DEPRECATION")
        private val legacyListener =
            object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    signalStrength ?: return
                    render(signalStrength.level)
                }
            }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (!hasPermission()) {
                renderUnknown()
                return
            }
            startListening()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            stopListening()
        }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            // The permission can be granted while this sits behind the settings screen, and no
            // reading arrives until something starts listening for one.
            if (visibility == VISIBLE && !listening && hasPermission()) startListening()
        }

        private fun hasPermission(): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED

        @Suppress("DEPRECATION")
        private fun startListening() {
            val telephony = context.getSystemService<TelephonyManager>() ?: return
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && callback != null) {
                    telephony.registerTelephonyCallback(context.mainExecutor, callback)
                } else {
                    telephony.listen(
                        legacyListener,
                        PhoneStateListener.LISTEN_SIGNAL_STRENGTHS,
                    )
                }
                listening = true
            }
        }

        @Suppress("DEPRECATION")
        private fun stopListening() {
            if (!listening) return
            val telephony = context.getSystemService<TelephonyManager>() ?: return
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && callback != null) {
                    telephony.unregisterTelephonyCallback(callback)
                } else {
                    telephony.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
                }
            }
            listening = false
        }

        /** @param level as the platform gives it: 0 for none through 4 for full. */
        private fun render(level: Int) {
            alpha = 1f
            setImageResource(
                when (level.coerceIn(0, 4)) {
                    0 -> R.drawable.ic_tabler_cell_signal_1
                    1 -> R.drawable.ic_tabler_cell_signal_2
                    2 -> R.drawable.ic_tabler_cell_signal_3
                    3 -> R.drawable.ic_tabler_cell_signal_4
                    else -> R.drawable.ic_tabler_cell_signal_5
                },
            )
            contentDescription =
                if (level <= 0) {
                    context.getString(R.string.signal_none_accessibility)
                } else {
                    context.getString(R.string.signal_level_accessibility, level)
                }
        }

        private fun renderUnknown() {
            setImageResource(R.drawable.ic_tabler_cell_signal_1)
            alpha = UNKNOWN_ALPHA
            contentDescription = context.getString(R.string.signal_unknown_accessibility)
        }

        private companion object {
            const val UNKNOWN_ALPHA = 0.45f
        }
    }
