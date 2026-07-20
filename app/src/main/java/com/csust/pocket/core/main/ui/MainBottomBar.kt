package com.csust.pocket.core.main.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csust.pocket.R
import com.csust.pocket.core.designsystem.hyperConcave
import com.csust.pocket.core.designsystem.hyperConvex
import com.csust.pocket.core.designsystem.hyperTap
import com.csust.pocket.core.main.navigation.MainTabDestination
import com.csust.pocket.core.theme.AppSkinTheme
import com.csust.pocket.core.theme.AppTheme
import com.csust.pocket.widget.view.FloatingTabBarScrollConnection
import com.csust.pocket.widget.view.rememberFloatingTabBarScrollConnection
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

/** Hyper-neumorphic bottom navigation: raised shell with an inset selected item. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun MainBottomBar(
    selectedDestination: MainTabDestination,
    onDestinationSelected: (MainTabDestination) -> Unit,
    scrollConnection: FloatingTabBarScrollConnection,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .hyperConvex(
                cornerRadius = 30.dp,
                elevation = 7.dp,
                surfaceColor = colors.bgPrimaryColor
            )
            .padding(7.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomBarItems.forEach { item ->
                val selected = item.index == selectedDestination.index
                val tint = if (selected) colors.commonColor else colors.secondaryTextColor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (selected) Modifier.hyperConcave(
                                cornerRadius = 22.dp,
                                surfaceColor = colors.bgButtonLowlightColor.copy(alpha = .72f)
                            ) else Modifier
                        )
                        .hyperTap {
                            MainTabDestination.fromIndex(item.index)?.let(onDestinationSelected)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.labelResId),
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = stringResource(item.labelResId),
                            color = tint,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private val bottomBarItems = listOf(
    MainBottomBarItem(0, Icons.Outlined.Home, R.string.overview),
    MainBottomBarItem(1, Icons.Outlined.GridView, R.string.function),
    MainBottomBarItem(2, Icons.Outlined.Person, R.string.profile_home)
)

private data class MainBottomBarItem(
    val index: Int,
    val icon: ImageVector,
    val labelResId: Int
)

@Preview(showBackground = true)
@Composable
private fun MainBottomBarPreview() {
    AppSkinTheme {
        MainBottomBar(
            selectedDestination = MainTabDestination.Overview,
            onDestinationSelected = {},
            scrollConnection = rememberFloatingTabBarScrollConnection(),
            hazeState = rememberHazeState()
        )
    }
}
