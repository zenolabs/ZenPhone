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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import app.baldphone.neo.data.Prefs

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.utils.BPrefs

/**
 * Colour family a tile belongs to.
 *
 * The grouping is what lets someone find a tile without reading it: communication is green,
 * media blue, tools purple, reminders amber, and the emergency button is red and alone.
 */
enum class TileAccent(
    @DrawableRes val backgroundRes: Int,
) {
    COMMS(R.drawable.bg_tile_green),
    MEDIA(R.drawable.bg_tile_blue),
    TOOLS(R.drawable.bg_tile_purple),
    REMINDERS(R.drawable.bg_tile_amber),
    EMERGENCY(R.drawable.bg_tile_red),
}

/**
 * Every tile the home screen can show, whichever page it happens to sit on today.
 *
 * This is the catalogue, not the layout: which tiles are visible and in what order lives in
 * [app.baldphone.neo.data.Prefs.homeTileOrder]. Until now the answer to both questions was
 * hard-coded in two XML files, which is why the fourth row had to be a special case and why
 * three tiles ended up duplicated across the two pages.
 *
 * @property id A stable identifier, persisted in preferences. Never rename these: an unknown
 *   id is dropped when the saved order is read, so a rename silently removes the tile from
 *   every existing installation.
 * @property customAppKey The legacy preference under which the user may have pointed this
 *   tile at an app of their own. Kept as-is so that existing assignments keep working.
 */
enum class HomeTile(
    val id: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val accent: TileAccent,
    val customAppKey: String?,
) {
    CONTACTS("contacts", R.string.contacts, R.drawable.ic_tabler_user, TileAccent.COMMS, BPrefs.CUSTOM_CONTACTS_KEY),
    RECENT("recent", R.string.recent, R.drawable.ic_tabler_history, TileAccent.COMMS, BPrefs.CUSTOM_RECENTS_KEY),
    DIALER("dialer", R.string.dialer, R.drawable.ic_tabler_phone, TileAccent.COMMS, BPrefs.CUSTOM_DIALER_KEY),

    CAMERA("camera", R.string.camera, R.drawable.ic_tabler_camera, TileAccent.MEDIA, BPrefs.CUSTOM_CAMERA_KEY),
    WHATSAPP("whatsapp", R.string.whatsapp, R.drawable.ic_tabler_brand_whatsapp, TileAccent.MEDIA, BPrefs.CUSTOM_APP_KEY),
    MESSAGES("messages", R.string.messages, R.drawable.ic_tabler_message, TileAccent.MEDIA, BPrefs.CUSTOM_MESSAGES_KEY),
    PHOTOS("photos", R.string.photos, R.drawable.ic_tabler_photo, TileAccent.MEDIA, null),
    VIDEOS("videos", R.string.videos, R.drawable.ic_tabler_movie, TileAccent.MEDIA, null),

    EMERGENCY("emergency", R.string.sos, R.drawable.ic_tabler_sos, TileAccent.EMERGENCY, BPrefs.CUSTOM_EMERGENCY_KEY),

    ASSISTANT("assistant", R.string.assistant, R.drawable.ic_tabler_microphone, TileAccent.TOOLS, BPrefs.CUSTOM_ASSISTANT_KEY),
    // The legacy key says "videos" because it was recycled when this tile changed purpose.
    // It stays with the lock tile: that is where any existing assignment actually belongs.
    LOCK_SCREEN("lock_screen", R.string.label_lock_screen_short, R.drawable.ic_tabler_lock, TileAccent.TOOLS, BPrefs.CUSTOM_VIDEOS_KEY),
    INTERNET("internet", R.string.internet, R.drawable.ic_tabler_world, TileAccent.TOOLS, null),
    MAPS("maps", R.string.maps, R.drawable.ic_tabler_map_2, TileAccent.TOOLS, null),
    SETTINGS("settings", R.string.settings, R.drawable.ic_tabler_settings, TileAccent.TOOLS, null),

    PILLS("pills", R.string.pills, R.drawable.ic_tabler_pill, TileAccent.REMINDERS, BPrefs.CUSTOM_PILLS_KEY),
    ALARMS("alarms", R.string.alarms, R.drawable.ic_tabler_alarm, TileAccent.REMINDERS, BPrefs.CUSTOM_ALARMS_KEY),
    APPS("apps", R.string.apps, R.drawable.ic_tabler_layout_grid, TileAccent.REMINDERS, BPrefs.CUSTOM_APPS_KEY),
    ;

    companion object {
        private val byId = entries.associateBy(HomeTile::id)

        /**
         * The most tiles the home screen will hold: four rows of three, which is what the old
         * fourth-row setting allowed at its largest.
         *
         * A limit is needed because the grid divides the height it has by the number of rows,
         * so every tile added makes all of them smaller. On a launcher built for people who do
         * not see well, that trade runs out well before the catalogue does.
         */
        const val MAX_TILES = 12

        fun fromId(id: String): HomeTile? = byId[id]

        /**
         * The tiles the home screen should show, in order.
         *
         * Ids that mean nothing here are dropped rather than treated as an error: they belong
         * to a version that knew about a tile this one does not. An empty result means the
         * layout has never been configured, and the defaults stand in.
         */
        @JvmStatic
        fun savedOrder(): List<HomeTile> {
            val saved = Prefs.homeTileOrder.mapNotNull(::fromId)
            return saved.ifEmpty { DEFAULT_ORDER }
        }

        /** Writes [tiles] back as the saved order. */
        @JvmStatic
        fun saveOrder(tiles: List<HomeTile>) {
            Prefs.homeTileOrder = tiles.map(HomeTile::id)
        }

        /**
         * What a fresh installation shows: the nine tiles of the old first page, in the order
         * they appeared there. Everything else starts out available but unused, so an existing
         * user upgrading sees exactly the home screen they had.
         */
        val DEFAULT_ORDER: List<HomeTile> =
            listOf(
                CONTACTS, RECENT, DIALER,
                CAMERA, WHATSAPP, MESSAGES,
                EMERGENCY, ASSISTANT, LOCK_SCREEN,
            )
    }
}
