package com.csust.pocket.core.theme

import androidx.appcompat.app.AppCompatDelegate
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/** Persists and applies the user's app-wide light/dark preference. */
object ThemeModeManager {
    private const val KEY_THEME_MODE = "app_theme_mode"

    private val _mode = MutableStateFlow(AppThemeMode.SYSTEM)
    val mode = _mode.asStateFlow()

    fun initialize() {
        val savedMode = runCatching {
            AppThemeMode.valueOf(
                MMKV.defaultMMKV().decodeString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
                    ?: AppThemeMode.SYSTEM.name
            )
        }.getOrDefault(AppThemeMode.SYSTEM)
        applyMode(savedMode, persist = false)
    }

    fun setMode(mode: AppThemeMode) {
        applyMode(mode, persist = true)
    }

    private fun applyMode(mode: AppThemeMode, persist: Boolean) {
        if (persist) MMKV.defaultMMKV().encode(KEY_THEME_MODE, mode.name)
        _mode.value = mode
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                AppThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                AppThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                AppThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
