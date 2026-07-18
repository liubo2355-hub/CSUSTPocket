package com.creamaker.changli_planet_app.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.creamaker.changli_planet_app.R
import com.creamaker.changli_planet_app.core.theme.AppTheme

/**
 * 二级页面统一返回入口：24dp 线性图标、44dp 可触控区域及一致的按压反馈。
 */
@Composable
fun PortalBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppTheme.colors.primaryTextColor
) {
    Box(modifier = modifier.width(52.dp).height(44.dp)) {
        HyperIconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            minSize = 44.dp
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_portal_back),
                contentDescription = "返回",
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
