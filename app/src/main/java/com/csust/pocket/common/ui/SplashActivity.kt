package com.csust.pocket.common.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.csust.pocket.R
import com.csust.pocket.core.PlanetApplication
import com.csust.pocket.core.Route
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 闪屏页，暂时启用
 */
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        // 使用协程来处理延迟任务
        lifecycleScope.launch {
//            if (PlanetApplication.Companion.accessToken.isNullOrEmpty() && !PlanetApplication.Companion.is_tourist) {
//                delay(300) // 延迟 0.2 秒
//                Route.goLogin(this@SplashActivity)
//            } else {
//                delay(200) // 延迟 0.2 秒
//                Route.goHome(this@SplashActivity)
//            }
            delay(200)
            Route.goHome(this@SplashActivity)

            finish()
        }
    }

}