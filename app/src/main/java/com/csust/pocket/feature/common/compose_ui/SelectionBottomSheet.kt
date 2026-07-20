package com.csust.pocket.feature.common.compose_ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.csust.pocket.core.theme.AppTheme

@Composable
fun SelectionBottomSheet(
    items: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = AppTheme.colors
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val maxHeightDp = with(density) {
        (windowInfo.containerSize.height * 0.5f).toDp()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() }
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeightDp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = colors.campusSnowColor,
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 8.dp)
                            .width(32.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colors.campusMistColor)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp)
                    ) {
                        items(items) { item ->
                            Text(
                                text = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item); onDismiss() }
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.W500,
                                color = colors.campusInkColor
                            )
                            if (item != items.last()) {
                                HorizontalDivider(
                                    color = colors.campusDividerColor,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}