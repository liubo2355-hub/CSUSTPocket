package com.csust.pocket.profileSettings.ui.model

sealed class SettingItemUiModel {
    data class Header(val title: String) : SettingItemUiModel()
    data class Option(
        val id: String,
        val title: String,
        val iconResId: Int? = null
    ) : SettingItemUiModel()
}