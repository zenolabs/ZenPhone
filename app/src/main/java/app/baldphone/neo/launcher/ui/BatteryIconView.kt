/*
 * Copyright 2025 Damian Kuzmiak
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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button

import androidx.annotation.DrawableRes
import androidx.annotation.UiThread
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import app.baldphone.neo.battery.BatteryRepository
import app.baldphone.neo.battery.BatteryState

import com.bald.uriah.baldphone.R

/**
 * Battery indicator drawn with the same outline icons as the rest of the top bar.
 *
 * This used to render five segments by hand on a custom outline. The drawing was good, but it
 * was the only thing in the bar that did not look like Tabler, so the level is now shown by
 * picking one of the stepped battery icons instead. Five steps, empty through full, is exactly
 * the granularity the segments offered, so nothing is lost.
 *
 * Everything else is unchanged:
 * - Critical low (<= 5%): red and blinking.
 * - Low: red, no blink.
 * - Charging: the charging icon, whatever the level.
 * - Full: green.
 * - Normal: the theme decoration colour.
 */
class BatteryIconView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.batteryIconViewStyle
    ) : View(context, attrs, defStyleAttr) {
        companion object {
            private const val BLINK_DURATION_MS = 800L
            private const val CRITICAL_LOW_LEVEL = 0.05f

            private const val DEFAULT_COLOR_NORMAL = Color.GRAY
            private const val DEFAULT_COLOR_LOW = Color.RED
            private const val DEFAULT_COLOR_FULL = Color.GREEN

            /**
             * Level thresholds, highest first. The icon changes as the reading crosses each
             * boundary, so the steps sit at the midpoints of the five buckets rather than at
             * round numbers: a 20% reading should not already show an empty battery.
             */
            private val LEVEL_STEPS =
                listOf(
                    0.875f to R.drawable.ic_tabler_battery_4,
                    0.625f to R.drawable.ic_tabler_battery_3,
                    0.375f to R.drawable.ic_tabler_battery_2,
                    0.125f to R.drawable.ic_tabler_battery_1,
                )
        }

        private var iconDrawable: Drawable? = null
        private var currentIconRes: Int = 0
        private val iconBounds = Rect()

        private var internalAlpha by Delegates.observable(255) { _, old, new ->
            if (old == new) return@observable
            iconDrawable?.alpha = new
            postInvalidateOnAnimation()
        }

        private var colorNormal = DEFAULT_COLOR_NORMAL
        private var colorLow = DEFAULT_COLOR_LOW
        private var colorFull = DEFAULT_COLOR_FULL
        private var colorCharging = DEFAULT_COLOR_NORMAL

        private var batteryLevel: Float = 0f
        private var mode: Mode = Mode.NORMAL

        private enum class Mode { NORMAL, LOW, CRITICAL_LOW, FULL, CHARGING }

        private var viewScope: CoroutineScope? = null
        private var blinkJob: Job? = null

        private var lastBatteryState: BatteryState? = null

        val detailedContentDescription: String
            get() = lastBatteryState?.formatInfo(context) ?: ""

        init {
            context.withStyledAttributes(attrs, R.styleable.BatteryIconView, defStyleAttr, 0) {
                colorNormal = getColor(R.styleable.BatteryIconView_batteryNormalColor, DEFAULT_COLOR_NORMAL)
                colorLow = getColor(R.styleable.BatteryIconView_batteryLowColor, DEFAULT_COLOR_LOW)
                colorFull = getColor(R.styleable.BatteryIconView_batteryFullColor, DEFAULT_COLOR_FULL)
                colorCharging = getColor(R.styleable.BatteryIconView_batteryChargingColor, colorNormal)
            }

            updateIcon()
        }

        /**
         * Updates the view state based on the provided [BatteryState].
         */
        @UiThread
        fun setBatteryState(batteryState: BatteryState) {
            if (lastBatteryState == batteryState) return

            lastBatteryState = batteryState
            val percentage = batteryState.percentage
            batteryLevel = (percentage ?: 0) / 100f

            val newMode =
                when {
                    batteryState.isFull -> Mode.FULL
                    batteryState.isCharging -> Mode.CHARGING
                    percentage != null && batteryLevel <= CRITICAL_LOW_LEVEL -> Mode.CRITICAL_LOW
                    batteryState.isLow -> Mode.LOW
                    else -> Mode.NORMAL
                }

            contentDescription = batteryState.formatSimpleInfo(context)

            val modeChanged = newMode != mode
            mode = newMode

            updateIcon()
            if (modeChanged) updateAnimationState()
        }

        /**
         * Binds this view to the [BatteryRepository], automatically updating its state in sync with the lifecycle.
         */
        fun observeBatteryState(lifecycleOwner: LifecycleOwner) {
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    BatteryRepository.get(context).batteryState.collect { state ->
                        setBatteryState(state)
                    }
                }
            }
        }

        @DrawableRes
        private fun iconForCurrentState(): Int =
            when (mode) {
                Mode.CHARGING -> R.drawable.ic_tabler_battery_charging
                Mode.FULL -> R.drawable.ic_tabler_battery_4
                else ->
                    LEVEL_STEPS.firstOrNull { batteryLevel > it.first }?.second
                        ?: R.drawable.ic_tabler_battery
            }

        private fun tintForCurrentMode(): Int =
            when (mode) {
                Mode.NORMAL -> colorNormal
                Mode.LOW, Mode.CRITICAL_LOW -> colorLow
                Mode.FULL -> colorFull
                Mode.CHARGING -> colorCharging
            }

        private fun updateIcon() {
            val wanted = iconForCurrentState()
            if (wanted != currentIconRes || iconDrawable == null) {
                currentIconRes = wanted
                iconDrawable = AppCompatResources.getDrawable(context, wanted)?.mutate()
                if (!iconBounds.isEmpty) iconDrawable?.bounds = iconBounds
            }

            iconDrawable?.apply {
                setTint(tintForCurrentMode())
                alpha = internalAlpha
            }
            invalidate()
        }

        private fun updateAnimationState() {
            if (mode == Mode.CRITICAL_LOW && canAnimate()) {
                if (blinkJob?.isActive != true) startBlinking()
            } else {
                stopBlinking()
            }
        }

        private fun startBlinking() {
            blinkJob =
                viewScope?.launch {
                    try {
                        while (isActive && canAnimate() && mode == Mode.CRITICAL_LOW) {
                            internalAlpha = if (internalAlpha == 255) 0 else 255
                            delay(BLINK_DURATION_MS)
                        }
                    } finally {
                        // Ensure icon is visible when coming back
                        internalAlpha = 255
                    }
                }
        }

        private fun stopBlinking() {
            blinkJob?.cancel()
            if (blinkJob == null && internalAlpha != 255) internalAlpha = 255
            blinkJob = null
        }

        private fun canAnimate(): Boolean = isShown && windowVisibility == VISIBLE

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int
        ) {
            if (w == oldw && h == oldh) return

            val contentW = (w - paddingLeft - paddingRight).toFloat()
            val contentH = (h - paddingTop - paddingBottom).toFloat()
            if (contentW <= 0 || contentH <= 0) return

            val size = minOf(contentW, contentH)
            val left = paddingLeft + (contentW - size) / 2f
            val top = paddingTop + (contentH - size) / 2f

            RectF(left, top, left + size, top + size).roundOut(iconBounds)
            iconDrawable?.bounds = iconBounds
        }

        override fun onDraw(canvas: Canvas) {
            iconDrawable?.draw(canvas)
        }

        // Standard View lifecycle hooks to trigger animation state updates
        override fun onAttachedToWindow() {
            super.onAttachedToWindow()

            viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            updateAnimationState()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()

            viewScope?.cancel()
            viewScope = null
            internalAlpha = 255
        }

        // This or parent visibility changed
        override fun onVisibilityChanged(
            v: View,
            visibility: Int
        ) {
            super.onVisibilityChanged(v, visibility)
            updateAnimationState()
        }

        // App foreground/background
        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            updateAnimationState()
        }

        override fun getAccessibilityClassName(): CharSequence = Button::class.java.name

        override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(info)
            info.className = Button::class.java.name
        }
    }
