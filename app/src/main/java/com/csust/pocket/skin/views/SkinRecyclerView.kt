package com.csust.pocket.skin.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.RecyclerView
import com.csust.pocket.skin.SkinAttributeProvider
import com.csust.pocket.skin.SkinManager
import com.csust.pocket.skin.SkinSupportable
import com.csust.pocket.skin.delegate.SkinDelegate

class SkinRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), SkinSupportable, SkinAttributeProvider {

    private val delegate = SkinDelegate(this, this)

    init {
        delegate.loadSkinAttributes(context, attrs)
        applySkin()
    }

    override fun loadSkinAttributes(context: Context, attrs: AttributeSet?) {
        // 只捕获 android:background 属性
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.background)) {
            val bgResId = getResourceId(0, 0)
            if (bgResId != 0) {
                // 复用通用的 "view_background" key
                delegate.setAttr("view_background", bgResId)
            }
        }
    }

    override fun applySkin() {
        delegate.applySkin()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 仅注册，不调用 applySkin()，防止列表滑动或重建时的闪烁/状态重置
        SkinManager.attach(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        SkinManager.detach(this)
    }
}