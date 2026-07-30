package app.baldphone.neo.settings

import androidx.annotation.StringRes

sealed class SettingId {
    object Accessibility : SettingId()

    object Appearance : SettingId()

    object Calls : SettingId()

    object Feedback : SettingId()

    object Help : SettingId()

    object Home : SettingId()

    object System : SettingId()
}

data class Item(
    val id: SettingId,
    @StringRes val titleRes: Int,
    val iconRes: Int? = null
)
