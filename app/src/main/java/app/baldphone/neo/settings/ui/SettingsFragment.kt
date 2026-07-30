package app.baldphone.neo.settings.ui

import android.content.Intent
import android.os.Bundle
import android.view.View

import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

import app.baldphone.neo.settings.Item
import app.baldphone.neo.settings.SettingId
import app.baldphone.neo.settings.SettingsAdapter

import app.baldphone.neo.activities.FeedbackActivity

import com.bald.uriah.baldphone.R

class SettingsFragment : Fragment(R.layout.fragment_settings_list) {
    private val items =
        listOf(
            Item(SettingId.Home, R.string.settings_home_screen, R.drawable.ic_tabler_layout_grid),
            Item(SettingId.Appearance, R.string.settings_appearance, R.drawable.ic_tabler_palette),
            Item(
                SettingId.Accessibility,
                R.string.accessibility_settings,
                R.drawable.ic_tabler_accessible
            ),
            Item(SettingId.Calls, R.string.settings_section_calls, R.drawable.phone_on_button),
            Item(SettingId.System, R.string.settings_system, R.drawable.settings_on_button),
            Item(SettingId.Help, R.string.about, R.drawable.ic_info),
            Item(SettingId.Feedback, R.string.feedback, R.drawable.ic_feedback)
        )

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SettingsAdapter(items) { handleSettingClick(it) }

        view.findViewById<RecyclerView>(R.id.recyclerView).apply {
            this.adapter = adapter

            val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            ContextCompat.getDrawable(requireContext(), R.drawable.ll_divider)?.let {
                divider.setDrawable(it)
                addItemDecoration(divider)
            }
        }
    }

    private fun handleSettingClick(id: SettingId) {
        // Feedback is the one entry that leaves the settings graph entirely, so it cannot be
        // expressed as a navigation action like the rest.
        if (id is SettingId.Feedback) {
            startActivity(Intent(requireContext(), FeedbackActivity::class.java))
            return
        }

        @IdRes
        val actionId =
            when (id) {
                is SettingId.Home -> R.id.action_settings_to_home
                is SettingId.Appearance -> R.id.action_settings_to_appearance
                is SettingId.Accessibility -> R.id.action_settings_to_accessibility
                is SettingId.Calls -> R.id.action_settings_to_calls
                is SettingId.System -> R.id.action_settings_to_system
                is SettingId.Help -> R.id.action_settings_to_suppport
                is SettingId.Feedback -> return
            }
        findNavController().navigate(actionId)
    }
}
