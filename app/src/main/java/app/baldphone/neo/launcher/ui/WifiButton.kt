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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.AttributeSet

import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.getSystemService

import com.bald.uriah.baldphone.R

/**
 * Says whether Wi-Fi is on, and whether it is actually carrying anything.
 *
 * Three states rather than two, because "on" and "working" are different things and the
 * difference is exactly what someone is being asked about down the phone. A Wi-Fi that is
 * switched on but connected to nothing looks identical to a working one on every indicator
 * that only reports the switch, and that is the case where the person is told they must be
 * mistaken.
 *
 * Nothing here toggles anything. Since Android 10 an app may not turn Wi-Fi on or off, and
 * the replacement is better suited to this phone anyway: the system's own panel, which is
 * large, familiar, and the same one the person would be talked through over the telephone.
 */
class WifiButton
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = android.R.attr.imageButtonStyle,
    ) : AppCompatImageButton(context, attrs, defStyleAttr) {

        private var wifiEnabled = false
        private var wifiCarrying = false

        /**
         * Watches the default network rather than polling. Callbacks arrive off the main
         * thread, so every one of them hands back before touching the view.
         */
        private val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    post { onNetwork(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) }
                }

                override fun onLost(network: Network) {
                    post { onNetwork(false) }
                }

                override fun onUnavailable() {
                    post { onNetwork(false) }
                }
            }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            context.getSystemService<ConnectivityManager>()
                ?.registerDefaultNetworkCallback(networkCallback)
            refresh()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            // Unregistering one that never registered throws, and a detach can follow a failed
            // attach, so the failure is expected rather than exceptional.
            runCatching {
                context.getSystemService<ConnectivityManager>()
                    ?.unregisterNetworkCallback(networkCallback)
            }
        }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            // The switch can be thrown in the system panel, which reports no network change at
            // all when Wi-Fi is turned on and connects to nothing.
            if (visibility == VISIBLE) refresh()
        }

        private fun onNetwork(carrying: Boolean) {
            wifiCarrying = carrying
            refresh()
        }

        /** Re-reads the switch; the carrying state comes from the callback. */
        private fun refresh() {
            wifiEnabled = context.getSystemService<WifiManager>()?.isWifiEnabled == true
            if (!wifiEnabled) wifiCarrying = false
            render()
        }

        private fun render() {
            setImageResource(
                if (wifiEnabled) R.drawable.ic_tabler_wifi else R.drawable.ic_tabler_wifi_off,
            )
            // Dimmed while switched on but connected to nothing: the shape says Wi-Fi exists,
            // the weight says whether to trust it.
            alpha = if (wifiEnabled && !wifiCarrying) DIMMED else 1f
            contentDescription =
                context.getString(
                    when {
                        !wifiEnabled -> R.string.wifi_off_accessibility
                        wifiCarrying -> R.string.wifi_connected_accessibility
                        else -> R.string.wifi_on_not_connected_accessibility
                    },
                )
        }

        private companion object {
            const val DIMMED = 0.45f
        }
    }
