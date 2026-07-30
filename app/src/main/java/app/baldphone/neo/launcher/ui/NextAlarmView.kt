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

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

import java.util.Calendar
import java.util.Date

import com.bald.uriah.baldphone.R

/**
 * Says when the next alarm will go off, and says nothing at all when none is set.
 *
 * Absent rather than empty when there is no alarm: a row reading "none" is a thing to read and
 * dismiss every time the home screen is looked at, and this screen is looked at all day.
 *
 * The alarm is asked of Android rather than of this launcher's own database, so an alarm set in
 * any clock app on the phone shows here too. Someone checking whether they will be woken
 * tomorrow does not care which app was used to arrange it, and would be badly served by an
 * indicator that stayed silent because the alarm was set elsewhere.
 */
class NextAlarmView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : LinearLayout(context, attrs, defStyleAttr) {

        private val time: TextView

        /** Android announces this whenever the next alarm changes, whoever changed it. */
        private val alarmChanged =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) = refresh()
            }

        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            LayoutInflater.from(context).inflate(R.layout.view_next_alarm, this, true)
            time = findViewById(R.id.next_alarm_time)
            visibility = GONE
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            ContextCompat.registerReceiver(
                context,
                alarmChanged,
                IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            refresh()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            // Detach can happen without a matching attach having registered, so a failure here
            // is expected rather than exceptional.
            runCatching { context.unregisterReceiver(alarmChanged) }
        }

        override fun onWindowVisibilityChanged(visibility: Int) {
            super.onWindowVisibilityChanged(visibility)
            // Broadcasts arrive while the launcher sits behind another app, but an alarm can
            // also have simply gone off in the meantime, which is not announced. Coming back
            // into view is the moment to look again.
            if (visibility == VISIBLE) refresh()
        }

        private fun refresh() {
            val next = context.getSystemService<AlarmManager>()?.nextAlarmClock
            if (next == null) {
                visibility = GONE
                return
            }

            val shown = format(next.triggerTime)
            time.text = shown
            contentDescription = context.getString(R.string.next_alarm_accessibility, shown)
            visibility = VISIBLE
        }

        /**
         * The time on its own for an alarm later today, the weekday in front of it otherwise.
         *
         * "07:30" with no other word means tomorrow morning to most people, and on a Friday
         * evening that reading is wrong three nights out of seven.
         */
        private fun format(triggerTime: Long): String {
            val clockTime =
                android.text.format.DateFormat
                    .getTimeFormat(context)
                    .format(Date(triggerTime))

            val now = Calendar.getInstance()
            val then = Calendar.getInstance().apply { timeInMillis = triggerTime }
            val sameDay =
                now.get(Calendar.ERA) == then.get(Calendar.ERA) &&
                    now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
            if (sameDay) return clockTime

            val weekday =
                android.text.format.DateUtils.formatDateTime(
                    context,
                    triggerTime,
                    android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY or
                        android.text.format.DateUtils.FORMAT_ABBREV_WEEKDAY,
                )
            return "$weekday $clockTime"
        }
    }
