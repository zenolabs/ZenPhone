package app.baldphone.neo.settings.ui

import android.media.Ringtone
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar

import app.baldphone.neo.data.Prefs
import app.baldphone.neo.settings.BaseSettingsFragment
import app.baldphone.neo.settings.SettingsRows
import app.baldphone.neo.ui.dialogs.BaldDialog
import app.baldphone.neo.views.SettingsSwitchButton

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.activities.alarms.AlarmScreenActivity

class CallsSettingsFragment : BaseSettingsFragment(R.layout.fragment_calls_settings) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val btConfirmCalls = view.findViewById<SettingsSwitchButton>(R.id.bt_confirm_calls)
        btConfirmCalls.apply {
            setChecked(Prefs.shouldConfirmCalls)
            setOnCheckedChangeListener { isChecked ->
                Prefs.shouldConfirmCalls = isChecked
            }
        }

        SettingsRows.bindSwitch(
            row = view.findViewById(R.id.row_dialer_sounds),
            titleRes = R.string.dialer_sounds,
            subtitleRes = R.string.dialer_sounds_subtext,
            iconRes = R.drawable.ic_tabler_volume,
            isChecked = Prefs.areDialerSoundsEnabled,
        ) { enabled -> Prefs.areDialerSoundsEnabled = enabled }

        SettingsRows.bindSwitch(
            row = view.findViewById(R.id.row_dual_sim),
            titleRes = R.string.dual_sim,
            subtitleRes = R.string.dual_sim_subtext,
            iconRes = R.drawable.ic_tabler_device_sim,
            isChecked = Prefs.isDualSimActive,
        ) { enabled -> Prefs.isDualSimActive = enabled }

        SettingsRows.bindAction(
            row = view.findViewById(R.id.row_alarm_volume),
            titleRes = R.string.alarm_volume,
            iconRes = R.drawable.ic_tabler_volume,
        ) { showAlarmVolumeDialog() }
    }

    /**
     * The slider plays the alarm at the chosen level as it moves, because a number on a scale
     * of five means nothing to someone deciding whether they will hear it in the morning. The
     * preview is stopped after a few seconds, and again whenever the slider moves, so dragging
     * across the range does not stack up overlapping ringtones.
     */
    private fun showAlarmVolumeDialog() {
        val context = requireContext()
        val seekBar =
            LayoutInflater.from(context).inflate(R.layout.volume_seek_bar, null, false) as SeekBar
        seekBar.progress = Prefs.alarmVolume

        val handler = Handler(Looper.getMainLooper())
        var preview: Ringtone? = null
        val stopPreview =
            Runnable {
                preview?.stop()
                preview = null
            }

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    handler.removeCallbacks(stopPreview)
                    stopPreview.run()

                    // Written before the ringtone is built: that is where the level is read.
                    Prefs.alarmVolume = progress
                    preview = AlarmScreenActivity.getRingtone(context)
                    preview?.play()
                    handler.postDelayed(stopPreview, PREVIEW_MILLIS)
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit

                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            },
        )

        BaldDialog
            .Builder(context)
            .setTitle(R.string.alarm_volume)
            .setMessage(R.string.alarm_volume_subtext)
            .setIcon(R.drawable.ic_tabler_volume)
            .setCustomView(seekBar)
            .setPositiveButton(context.getText(R.string.ok))
            .setOnDismissListener {
                handler.removeCallbacks(stopPreview)
                stopPreview.run()
            }
            .show()
    }

    private companion object {
        const val PREVIEW_MILLIS = 4_000L
    }
}
