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

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner

import app.baldphone.neo.features.notifications.ui.NotificationsActivity
import app.baldphone.neo.launcher.ui.FlashlightButton
import app.baldphone.neo.launcher.ui.MobileSignalView
import app.baldphone.neo.launcher.ui.NotificationsButton
import app.baldphone.neo.launcher.ui.SoundButton
import app.baldphone.neo.launcher.ui.ThemeButton
import app.baldphone.neo.launcher.ui.WifiButton
import app.baldphone.neo.permissions.PermissionManager
import app.baldphone.neo.ui.dialogs.BrightnessDialog
import app.baldphone.neo.utils.startActivitySafe

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.SOSActivity

/**
 * The strip along the top of the home screen, built from what has been chosen rather than from
 * a layout file.
 *
 * It used to be four views nailed into a ConstraintLayout chain, each wired up by hand in the
 * activity. That worked while the four could not change; it cannot answer "show me these
 * three instead". This is the same move already made on the home tiles, and for the same
 * reason.
 *
 * Every item gets an equal share of the bar, which is why the container decides the layout
 * parameters rather than each view bringing its own.
 */
class TopBarView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : LinearLayout(context, attrs, defStyleAttr) {

        /**
         * Fills the bar and wires up what is in it.
         *
         * @param requestFlashlightPermission the torch needs the camera, and asking for it
         *   belongs to the activity, which owns the permission machinery.
         */
        fun bind(
            owner: LifecycleOwner,
            requestFlashlightPermission: (Runnable) -> Unit,
        ) {
            removeAllViews()

            for (item in TopBarItem.savedOrder()) {
                val view = createView(item) ?: continue
                styleAsBarButton(view)
                bindView(item, view, owner, requestFlashlightPermission)
                addView(view, barLayoutParams())
            }
        }

        /**
         * Null for an item whose view does not exist yet, so that a choice saved by a later
         * version leaves a gap rather than bringing the bar down.
         */
        private fun createView(item: TopBarItem): View? =
            when (item) {
                TopBarItem.WIFI -> WifiButton(context)
                TopBarItem.MOBILE_SIGNAL -> MobileSignalView(context)
                TopBarItem.FLASHLIGHT -> FlashlightButton(context)
                TopBarItem.SOUND -> SoundButton(context)
                TopBarItem.BRIGHTNESS -> AppCompatImageButton(context)
                TopBarItem.THEME -> ThemeButton(context)
                TopBarItem.NOTIFICATIONS -> NotificationsButton(context)
                TopBarItem.SOS -> AppCompatImageButton(context)
            }

        private fun bindView(
            item: TopBarItem,
            view: View,
            owner: LifecycleOwner,
            requestFlashlightPermission: (Runnable) -> Unit,
        ) {
            when (item) {
                TopBarItem.WIFI ->
                    view.setOnClickListener { openInternetPanel() }

                TopBarItem.FLASHLIGHT ->
                    (view as FlashlightButton).bind(owner, requestFlashlightPermission)

                TopBarItem.SOUND ->
                    (view as SoundButton).bind(owner)

                TopBarItem.NOTIFICATIONS -> {
                    (view as NotificationsButton).bind(owner)
                    view.setOnClickListener {
                        context.startActivitySafe(Intent(context, NotificationsActivity::class.java))
                    }
                }

                // The theme button switches on its own and reports its own state; there is
                // nothing here that it needs from the activity.
                TopBarItem.THEME -> Unit

                TopBarItem.MOBILE_SIGNAL -> {
                    // It watches the signal itself. Tapping opens the phone's own network
                    // settings, which is where an unanswered "have I got any signal?" leads.
                    view.contentDescription = context.getString(item.labelRes)
                    view.setOnClickListener {
                        context.startActivitySafe(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                    }
                }

                TopBarItem.BRIGHTNESS -> {
                    (view as AppCompatImageButton).setImageResource(R.drawable.ic_tabler_brightness)
                    view.imageTintList = barIconTint()
                    view.contentDescription = context.getString(item.labelRes)
                    view.setOnClickListener { openBrightness() }
                }

                TopBarItem.SOS -> {
                    (view as AppCompatImageButton).setImageResource(R.drawable.ic_tabler_sos)
                    view.imageTintList = barIconTint()
                    view.contentDescription = context.getString(item.labelRes)
                    // Opens the emergency screen, as the tile does. It does not place a call:
                    // this button sits where a thumb rests when the phone is picked up.
                    view.setOnClickListener {
                        context.startActivitySafe(Intent(context, SOSActivity::class.java))
                    }
                }
            }
        }

        /**
         * Brightness is a system setting, so it cannot be offered until the permission to write
         * one is in hand; the dialog would otherwise come up looking usable and save nothing.
         */
        private fun openBrightness() {
            // FragmentActivity and not merely Activity: the permission machinery asks through a
            // fragment, which is also why this cannot be done from a bare context.
            val activity = context as? FragmentActivity ?: return
            PermissionManager.checkOrRequest(activity, PermissionManager.WRITE_SETTINGS) {
                onGranted { BrightnessDialog.show(activity) }
            }
        }

        /**
         * Hands the switching to Android rather than doing it here, which since Android 10 no
         * app may. The panel is also the one the person would be talked through on the phone.
         */
        private fun openInternetPanel() {
            val intent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
            context.startActivitySafe(intent)
        }

        /**
         * The bar's own icon colour.
         *
         * Applied only to the two drawn from shared drawables that carry a colour meant for
         * somewhere else - the SOS is coloured for a tile, brightness for a settings row.
         * Duplicating the files to change one attribute would leave two copies to keep in
         * step; tinting where they are used leaves one.
         */
        private fun barIconTint(): ColorStateList {
            val resolved = TypedValue()
            context.theme.resolveAttribute(R.attr.bald_decoration_on_background, resolved, true)
            return ColorStateList.valueOf(resolved.data)
        }

        private fun styleAsBarButton(view: View) {
            view.setBackgroundResource(R.drawable.style_for_buttons_transparent)
            val padding = resources.getDimensionPixelSize(R.dimen.top_bar_icon_padding)
            view.setPadding(padding, padding, padding, padding)
            (view as? ImageView)?.scaleType = ImageView.ScaleType.FIT_CENTER
        }

        /**
         * An equal share each, along whichever way the bar runs. The orientation comes from the
         * layout, so portrait and landscape differ only there and not in this code.
         */
        private fun barLayoutParams(): LayoutParams =
            if (orientation == HORIZONTAL) {
                LayoutParams(0, resources.getDimensionPixelSize(R.dimen.top_bar_height), 1f)
            } else {
                LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            }
    }
