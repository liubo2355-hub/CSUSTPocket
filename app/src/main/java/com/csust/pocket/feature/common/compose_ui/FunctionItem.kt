package com.csust.pocket.feature.common.compose_ui


import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csust.pocket.R
import com.csust.pocket.core.theme.AppTheme

/**
 * 带有背景色的小卡片，带图片
 */
@Immutable
data class FunctionItemData(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val tintColor: Color = Color.Unspecified,
    val onClick: () -> Unit
)

/**
 * 单个功能项组件
 */
@Composable
fun FunctionItem(
    item: FunctionItemData,
    modifier: Modifier = Modifier
) {
    var isClicked by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isClicked) 0.92f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale",
        finishedListener = { isClicked = false }
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                onClick = {
                    isClicked = true
                    item.onClick()
                }
            )
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(item.tintColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = functionIconForId(item.id),
                contentDescription = item.title,
                tint = item.tintColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            color = AppTheme.colors.primaryTextColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp
        )
    }
}

@Preview
@Composable
fun FunctionItemPreview() {
    FunctionItem(
        item = FunctionItemData(
            "schedule",
            "课表",
            R.drawable.ic_timetable,
            FunctionColors.Schedule
        ) {},
    )
}
