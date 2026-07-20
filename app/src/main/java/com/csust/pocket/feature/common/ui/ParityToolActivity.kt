package com.csust.pocket.feature.common.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csust.pocket.R
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.feature.common.data.WebVpnUrlCodec
import com.csust.pocket.core.designsystem.HyperIconButton
import com.csust.pocket.core.designsystem.PortalBackButton
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.feature.common.ui.compose.GradeAnalysisScreen

class ParityToolActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tool = intent.getStringExtra(EXTRA_TOOL).orEmpty()
        setContent {
            AppSkinTheme {
                ParityToolScreen(tool = tool, onBack = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_TOOL = "tool"
        const val TOOL_GRADE_ANALYSIS = "grade_analysis"
        const val TOOL_WEBVPN = "webvpn"
    }
}

@Composable
private fun ParityToolScreen(tool: String, onBack: () -> Unit) {
    val title = if (tool == ParityToolActivity.TOOL_WEBVPN) "WebVPN 转换" else "成绩分析"
    Column(Modifier.fillMaxSize().background(AppTheme.colors.bgPrimaryColor).statusBarsPadding()) {
        HyperSurface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                PortalBackButton(onClick = onBack)
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp))
            }
        }
        if (tool == ParityToolActivity.TOOL_WEBVPN) WebVpnConverterContent() else GradeAnalysisScreen()
    }
}

@Composable
private fun WebVpnConverterContent() {
    var convertMode by remember { mutableStateOf(true) }
    var input by remember { mutableStateOf("") }
    val result = remember(input, convertMode) {
        if (input.isBlank()) "" else runCatching {
            if (convertMode) WebVpnUrlCodec.encrypt(input) else WebVpnUrlCodec.decrypt(input)
        }.getOrElse { "转换失败：${it.message}" }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeButton("转换", convertMode, Modifier.weight(1f)) { convertMode = true }
            ModeButton("还原", !convertMode, Modifier.weight(1f)) { convertMode = false }
        }
        Text(if (convertMode) "原始链接" else "WebVPN 链接", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("请输入或粘贴链接") },
            minLines = 2
        )
        Text(if (convertMode) "WebVPN 链接" else "原始链接", fontWeight = FontWeight.Bold)
        Surface(shape = RoundedCornerShape(16.dp), color = AppTheme.colors.bgCardColor, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (result.isBlank()) "转换结果将显示在此处" else result,
                modifier = Modifier.padding(16.dp),
                color = if (result.startsWith("转换失败")) Color(0xFFD33C3C) else AppTheme.colors.primaryTextColor
            )
        }
        Text("功能说明", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("转换功能将普通校园内网链接转换为可通过长沙理工大学 WebVPN 访问的专属链接；还原功能可解析 WebVPN 链接中的原始地址。", color = AppTheme.colors.secondaryTextColor, lineHeight = 21.sp)
    }
}

@Composable
private fun ModeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF1697D5) else AppTheme.colors.bgCardColor
    ) {
        Text(text, color = if (selected) Color.White else AppTheme.colors.primaryTextColor, modifier = Modifier.padding(vertical = 11.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
