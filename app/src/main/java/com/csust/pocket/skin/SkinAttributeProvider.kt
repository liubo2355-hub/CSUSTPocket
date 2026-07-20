package com.csust.pocket.skin

import android.content.Context
import android.util.AttributeSet

interface SkinAttributeProvider {
    fun loadSkinAttributes(context: Context,attrs: AttributeSet?)
}