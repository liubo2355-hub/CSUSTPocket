package com.csust.pocket.utils.event

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * 基于 SharedFlow 的轻量级应用内事件总线，替代 EventBus。
 *
 * - 普通事件：extraBufferCapacity = 1，replay = 0（热流，不粘滞）
 * - Sticky 事件：replay = 1（新订阅者自动拿到最后一次值）
 */
object AppEventBus {
    /** 选择事件（课表弹窗选中后通知关闭弹窗 / Tab 切换） */
    val selectEvent = MutableSharedFlow<SelectEvent>(extraBufferCapacity = 1)

    /** 绑定用户成功后通知关闭 BindingUserActivity */
    val finishEvent = MutableSharedFlow<FinishEvent>(extraBufferCapacity = 1)

    /** 新鲜事模块事件（关闭发布页、刷新列表、打开评论等） */
}
