package com.creamaker.changli_planet_app.feature.common.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.core.designsystem.HyperSpacing
import com.creamaker.changli_planet_app.core.designsystem.HyperSurface
import com.creamaker.changli_planet_app.core.designsystem.PortalBackButton
import com.creamaker.changli_planet_app.core.theme.AppSkinTheme
import com.creamaker.changli_planet_app.core.theme.AppTheme

class ContractActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppSkinTheme {
                UserAgreementScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun UserAgreementScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.bgPrimaryColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = HyperSpacing.pageHorizontal)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 10.dp),
            shape = RoundedCornerShape(HyperSpacing.topBarRadius),
            color = AppTheme.colors.bgCardColor,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HyperSpacing.topBarContentHeight)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                PortalBackButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                    tint = Color(0xFF168FD0)
                )
                Text(
                    text = "用户协议",
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HyperSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = HyperSpacing.cardPaddingHorizontal,
                        vertical = HyperSpacing.cardPaddingVertical
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "“掌上长理”用户协议",
                        color = AppTheme.colors.primaryTextColor,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    AgreementSection(
                        "1. 协议确认",
                        "在使用“掌上长理”前，请仔细阅读本用户协议。当您继续使用本应用时，即表示您已阅读、理解并同意接受本协议的全部内容。若您不同意本协议的任何条款，请停止使用本应用。"
                    )
                    AgreementSection(
                        "2. 服务说明",
                        "本应用用于集中展示课表、成绩、考试、作业、宿舍电量等校园信息。部分数据来自学校相关系统，实际内容与可用性以学校官方系统为准。"
                    )
                    AgreementSection(
                        "3. 免责声明",
                        "本应用为个人开发项目，并非长沙理工大学官方应用。开发者不对校园系统调整、网络异常、第三方服务变化或不可抗力造成的服务中断及间接损失承担责任。"
                    )
                    AgreementSection(
                        "4. 数据与隐私",
                        "课表、成绩、账号及相关信息仅用于提供校园查询服务。敏感登录凭据保存在本地，不会作为公开内容上传；请妥善保管设备及账号信息。"
                    )
                    AgreementSection(
                        "5. 协议更新",
                        "为适应功能和服务变化，本协议可能进行更新。更新后的内容将在应用内展示，继续使用应用即视为接受更新后的协议。"
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AgreementSection(title: String, content: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = title,
            color = AppTheme.colors.primaryTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = content,
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}
