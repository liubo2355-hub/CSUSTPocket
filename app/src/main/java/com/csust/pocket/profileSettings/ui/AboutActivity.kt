package com.csust.pocket.profileSettings.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csust.pocket.BuildConfig
import com.csust.pocket.R
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.PortalBackButton
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppSkinTheme {
                AboutScreen(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(activity: Activity? = null) {
    Scaffold(
        containerColor = AppTheme.colors.bgPrimaryColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "关于掌上长理",
                        color = AppTheme.colors.titleTopColor,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    PortalBackButton(
                        onClick = { activity?.finish() },
                        tint = AppTheme.colors.titleTopColor
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.bgTopBarColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppTheme.colors.bgPrimaryColor)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Text(
                text = "掌上长理",
                color = AppTheme.colors.primaryTextColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "便捷、清晰的校园信息工具",
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))
            AboutSection(
                title = "应用介绍",
                content = "掌上长理是一款面向长沙理工大学师生的校园工具应用，提供课表、成绩、考试安排、网络课程、宿舍电量和校园服务等功能，帮助用户更便捷地获取与管理校园信息。"
            )
            Spacer(Modifier.height(14.dp))
            AboutSection(
                title = "免责声明",
                content = "掌上长理为非官方校园工具，与长沙理工大学及校内各业务系统不存在隶属或授权关系。应用展示的数据可能因网络、系统维护或接口调整出现延迟与差异，重要信息请以学校官方渠道为准。用户应妥善保管账号信息，并自行判断和承担使用相关功能产生的风险。"
            )

            Text(
                text = "版本 ${BuildConfig.VERSION_NAME}",
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 18.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun AboutSection(title: String, content: String) {
    HyperSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AppTheme.colors.bgCardColor
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = title,
                color = AppTheme.colors.primaryTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = content,
                color = AppTheme.colors.secondaryTextColor,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
