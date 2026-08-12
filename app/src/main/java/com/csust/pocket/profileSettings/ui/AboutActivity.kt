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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val licenseText = remember {
        context.resources.openRawResource(R.raw.mit_license)
            .bufferedReader()
            .use { it.readText() }
    }

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
                title = "项目来源与版权",
                content = "掌上长理基于 CreaMakers 的开源项目 changli-planet-app 进行二次开发。\n\nCopyright (c) 2026 CreaMakers\n\n原项目：$ORIGINAL_PROJECT_URL",
                actionLabel = "访问原项目",
                onAction = { uriHandler.openUri(ORIGINAL_PROJECT_URL) }
            )
            Spacer(Modifier.height(14.dp))
            AboutSection(
                title = "开源许可证（MIT License）",
                content = licenseText
            )
            Spacer(Modifier.height(14.dp))
            AboutSection(
                title = "后端服务说明",
                content = "应用当前默认使用由 CreaMakers 独立开发、部署和维护的后端服务。该在线服务不属于掌上长理，也不包含在 MIT License 的源代码授权中。其接口、访问策略、速率限制和可用性可能随时调整、限制、暂停或终止。掌上长理不得滥用、越权调用、批量抓取或向未授权第三方转授访问能力，并自行承担服务变化带来的兼容性和可用性风险。"
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
private fun AboutSection(
    title: String,
    content: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
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
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private const val ORIGINAL_PROJECT_URL =
    "https://github.com/CreaMakers/changli-planet-app"
