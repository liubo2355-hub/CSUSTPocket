package com.csust.pocket.skin.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csust.pocket.R
import com.csust.pocket.core.designsystem.HyperSpacing
import com.csust.pocket.core.designsystem.HyperSurface
import com.csust.pocket.core.designsystem.PortalBackButton
import com.csust.pocket.core.designsystem.hyperTap
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.core.theme.AppThemeMode
import com.csust.pocket.core.theme.ThemeModeManager

class SkinSelectionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppSkinTheme {
                ThemeModeScreen(onBackClick = { finish() })
            }
        }
    }
}

@Composable
private fun ThemeModeScreen(onBackClick: () -> Unit) {
    val selectedMode by ThemeModeManager.mode.collectAsState()

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
                    onClick = onBackClick,
                    modifier = Modifier.align(Alignment.CenterStart),
                    tint = Color(0xFF168FD0)
                )
                Text(
                    text = "外观主题",
                    color = AppTheme.colors.primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "显示模式",
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 8.dp)
        )

        HyperSurface(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = HyperSpacing.cardPaddingHorizontal)) {
                ThemeModeRow(
                    title = "浅色模式",
                    description = "始终使用浅色界面",
                    selected = selectedMode == AppThemeMode.LIGHT,
                    onClick = { ThemeModeManager.setMode(AppThemeMode.LIGHT) }
                )
                HorizontalDivider(color = AppTheme.colors.dividerColor.copy(alpha = 0.14f))
                ThemeModeRow(
                    title = "深色模式",
                    description = "始终使用深色界面",
                    selected = selectedMode == AppThemeMode.DARK,
                    onClick = { ThemeModeManager.setMode(AppThemeMode.DARK) }
                )
                HorizontalDivider(color = AppTheme.colors.dividerColor.copy(alpha = 0.14f))
                ThemeModeRow(
                    title = "跟随系统",
                    description = "随系统显示设置自动切换",
                    selected = selectedMode == AppThemeMode.SYSTEM,
                    onClick = { ThemeModeManager.setMode(AppThemeMode.SYSTEM) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "选择后立即生效，并会在下次启动时继续使用。",
            color = AppTheme.colors.secondaryTextColor,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
private fun ThemeModeRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .hyperTap(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AppTheme.colors.primaryTextColor, fontSize = 15.sp)
            Text(description, color = AppTheme.colors.secondaryTextColor, fontSize = 12.sp)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
