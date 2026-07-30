package app.baldphone.neo.settings

import androidx.annotation.StringRes

import com.bald.uriah.baldphone.R

sealed class SettingId {
    object Accessibility : SettingId()

    object Appearance : SettingId()

    object Calls : SettingId()

    object Feedback : SettingId()

    object Help : SettingId()

    object Home : SettingId()

    object System : SettingId()

    /**
     * A name that survives being put in an intent, so a section can be asked for from outside
     * the settings screen. Written out rather than taken from the class name, which a minifier
     * is free to change, and rather than an ordinal, which reorders the day someone reorders
     * the list.
     */
    val key: String
        get() =
            when (this) {
                Accessibility -> "accessibility"
                Appearance -> "appearance"
                Calls -> "calls"
                Feedback -> "feedback"
                Help -> "help"
                Home -> "home"
                System -> "system"
            }

    companion object {
        fun fromKey(key: String?): SettingId? =
            when (key) {
                "accessibility" -> Accessibility
                "appearance" -> Appearance
                "calls" -> Calls
                "feedback" -> Feedback
                "help" -> Help
                "home" -> Home
                "system" -> System
                else -> null
            }
    }
}

data class Item(
    val id: SettingId,
    @StringRes val titleRes: Int,
    val iconRes: Int? = null
)

/**
 * The settings menu, in the order it is shown.
 *
 * Kept here rather than inside the fragment that used to own it, because the home screen shows
 * the same list on the page to the left of the tiles. Two copies would be two lists to keep in
 * step, and the one that fell behind would be the one nobody was looking at.
 */
object SettingsMenu {
    val ITEMS: List<Item> =
        listOf(
            Item(SettingId.Home, R.string.settings_home_screen, R.drawable.ic_tabler_layout_grid),
            Item(SettingId.Appearance, R.string.settings_appearance, R.drawable.ic_tabler_palette),
            Item(
                SettingId.Accessibility,
                R.string.accessibility_settings,
                R.drawable.ic_tabler_accessible,
            ),
            Item(SettingId.Calls, R.string.settings_section_calls, R.drawable.phone_on_button),
            Item(SettingId.System, R.string.settings_system, R.drawable.settings_on_button),
            Item(SettingId.Help, R.string.about, R.drawable.ic_info),
            Item(SettingId.Feedback, R.string.feedback, R.drawable.ic_feedback),
        )
}
