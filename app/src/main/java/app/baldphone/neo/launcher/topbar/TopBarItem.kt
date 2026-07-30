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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import app.baldphone.neo.data.Prefs

import com.bald.uriah.baldphone.R

/**
 * Everything the strip along the top of the home screen can hold.
 *
 * Built on the same plan as [app.baldphone.neo.launcher.home.HomeTile], and for the same
 * reason: which of these appear is a decision made once by whoever sets the phone up, and it
 * belongs in a preference rather than in a layout file. The four that used to be nailed into
 * the layout are simply the four that come as standard.
 *
 * @property id A stable identifier, persisted in preferences. Never rename these: an unknown
 *   id is dropped when the saved choice is read, so a rename quietly removes the item from
 *   every phone already configured.
 */
enum class TopBarItem(
    val id: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val descriptionRes: Int,
) {
    WIFI("wifi", R.string.top_bar_wifi, R.drawable.ic_tabler_wifi, R.string.top_bar_wifi_subtext),
    MOBILE_SIGNAL(
        "mobile_signal",
        R.string.top_bar_mobile_signal,
        R.drawable.ic_tabler_cell_signal_5,
        R.string.top_bar_mobile_signal_subtext,
    ),
    FLASHLIGHT(
        "flashlight",
        R.string.top_bar_flashlight,
        R.drawable.ic_tabler_bulb,
        R.string.top_bar_flashlight_subtext,
    ),
    SOUND("sound", R.string.top_bar_sound, R.drawable.ic_tabler_volume, R.string.top_bar_sound_subtext),
    BRIGHTNESS(
        "brightness",
        R.string.top_bar_brightness,
        R.drawable.ic_tabler_brightness,
        R.string.top_bar_brightness_subtext,
    ),
    NOTIFICATIONS(
        "notifications",
        R.string.top_bar_notifications,
        R.drawable.ic_tabler_bell,
        R.string.top_bar_notifications_subtext,
    ),
    SOS("sos", R.string.top_bar_sos, R.drawable.ic_tabler_sos, R.string.top_bar_sos_subtext),
    ;

    companion object {
        private val byId = entries.associateBy(TopBarItem::id)

        /**
         * How many the bar will hold.
         *
         * Five. The icon is bound by the bar's height and not by its width, so a fifth does not
         * make any of them smaller to look at; it only narrows the target, from about a quarter
         * of the screen to about a fifth. On a phone of ordinary width that is still some 78dp
         * against the 48dp Android asks for, which leaves room for a hand that is not steady.
         *
         * Six would begin to bite, and the icons would be as small as the ones on Android's own
         * status bar, which is the thing this launcher exists to avoid.
         */
        const val MAX_ITEMS = 5

        fun fromId(id: String): TopBarItem? = byId[id]

        /**
         * What the bar should show, in order.
         *
         * Ids meaning nothing here are dropped rather than treated as an error: they belong to
         * a version that knew about an item this one does not. An empty result means the bar
         * has never been configured, and the four it has always had stand in.
         */
        /**
         * What the bar should show, left to right.
         *
         * The order is the one arranged in the settings and nothing else. It was briefly sorted
         * into catalogue order here, which was the right answer while the bar could not be
         * rearranged and the wrong one the moment it could.
         */
        @JvmStatic
        fun savedOrder(): List<TopBarItem> {
            val saved = Prefs.topBarOrder.mapNotNull(::fromId).take(MAX_ITEMS)
            return saved.ifEmpty { DEFAULT_ORDER }
        }

        @JvmStatic
        fun saveOrder(items: List<TopBarItem>) {
            Prefs.topBarOrder = items.map(TopBarItem::id)
        }

        /**
         * What the bar has shown since before it could be changed, left to right.
         *
         * Wi-Fi took the battery's place when the battery turned out to be saying beside the
         * clock what it was already saying here.
         */
        val DEFAULT_ORDER: List<TopBarItem> = listOf(WIFI, FLASHLIGHT, SOUND, NOTIFICATIONS)
    }
}
