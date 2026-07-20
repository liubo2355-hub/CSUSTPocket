package com.csust.pocket.profileSettings.ui.adapter.model

import androidx.annotation.DrawableRes

data class SettingItem(
    val title: String,
    @DrawableRes val iconResId: Int = 0,
    val isHeader: Boolean = false,
    val actionType: Int = 0
)
