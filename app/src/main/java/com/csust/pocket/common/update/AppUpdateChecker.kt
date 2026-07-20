package com.csust.pocket.common.update

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.csust.pocket.common.redux.action.UserAction
import com.csust.pocket.common.redux.store.UserStore
import com.csust.pocket.widget.view.CustomToast

/**
 * 应用更新检查的统一入口，确保自动检查和手动检查使用相同的版本信息与并发策略。
 */
object AppUpdateChecker {
    private val store by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { UserStore() }

    fun check(context: Context, manual: Boolean = false) {
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            if (manual) CustomToast.showMessage(context, "无法读取当前版本")
            return
        }

        store.dispatch(
            UserAction.QueryIsLastedApk(
                context = context,
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                versionName = packageInfo.versionName.orEmpty(),
                manual = manual
            )
        )
    }
}
