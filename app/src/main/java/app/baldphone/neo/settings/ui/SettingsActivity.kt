package app.baldphone.neo.settings.ui

import android.os.Bundle

import androidx.annotation.IdRes
import androidx.navigation.fragment.NavHostFragment

import app.baldphone.neo.activities.BaseActivity
import app.baldphone.neo.settings.SettingId

import com.bald.uriah.baldphone.R
import com.bald.uriah.baldphone.databinding.ActivitySettingsNeoBinding

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsNeoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsNeoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.label?.let { label ->
                binding.titleBar.setTitle(label.toString())
            }
        }

        // Only on the first creation. On a rotation the navigation controller restores wherever
        // the person had got to, and jumping back to the section they arrived at would undo it.
        if (savedInstanceState == null) {
            SettingId.fromKey(intent.getStringExtra(EXTRA_SECTION))?.let { section ->
                actionFor(section)?.let(navHostFragment.navController::navigate)
            }
        }
    }

    /**
     * The navigation action that opens a section.
     *
     * Feedback is absent on purpose: it leaves the settings graph for an activity of its own,
     * so it cannot be reached by navigating within it. Whoever asks for it from outside can
     * start that activity directly, which is what the menu itself does.
     */
    @IdRes
    private fun actionFor(section: SettingId): Int? =
        when (section) {
            is SettingId.Home -> R.id.action_settings_to_home
            is SettingId.Appearance -> R.id.action_settings_to_appearance
            is SettingId.Accessibility -> R.id.action_settings_to_accessibility
            is SettingId.Calls -> R.id.action_settings_to_calls
            is SettingId.System -> R.id.action_settings_to_system
            is SettingId.Help -> R.id.action_settings_to_suppport
            is SettingId.Feedback -> null
        }

    companion object {
        /**
         * Which section to open at, as a [SettingId.key]. Left out, the menu itself is shown.
         */
        const val EXTRA_SECTION = "section"
    }
}
