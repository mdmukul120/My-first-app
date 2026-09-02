package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SportsCyan
import com.example.ui.viewmodel.BottomNavTab

@Composable
fun MukulBottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    liveMatchCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mukul_bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = androidx.compose.ui.unit.Dp(8f)
    ) {
        // 1. Live Sports Tab
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.LIVE_SPORTS,
            onClick = { onTabSelected(BottomNavTab.LIVE_SPORTS) },
            icon = {
                if (liveMatchCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = LiveRed) {
                                Text(text = "$liveMatchCount", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectedTab == BottomNavTab.LIVE_SPORTS) Icons.Filled.SportsSoccer else Icons.Outlined.SportsSoccer,
                            contentDescription = "Live Sports"
                        )
                    }
                } else {
                    Icon(
                        imageVector = if (selectedTab == BottomNavTab.LIVE_SPORTS) Icons.Filled.SportsSoccer else Icons.Outlined.SportsSoccer,
                        contentDescription = "Live Sports"
                    )
                }
            },
            label = {
                Text(
                    text = "Live Sports",
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == BottomNavTab.LIVE_SPORTS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_live_sports")
        )

        // 2. Tapmad BD Tab
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.TAPMAD,
            onClick = { onTabSelected(BottomNavTab.TAPMAD) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == BottomNavTab.TAPMAD) Icons.Filled.LiveTv else Icons.Outlined.LiveTv,
                    contentDescription = "Tapmad BD"
                )
            },
            label = {
                Text(
                    text = "Tapmad BD",
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == BottomNavTab.TAPMAD) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_tapmad")
        )

        // 3. Live TV Tab
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.LIVE_TV,
            onClick = { onTabSelected(BottomNavTab.LIVE_TV) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == BottomNavTab.LIVE_TV) Icons.Filled.Tv else Icons.Outlined.Tv,
                    contentDescription = "Live TV"
                )
            },
            label = {
                Text(
                    text = "Live TV",
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == BottomNavTab.LIVE_TV) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_live_tv")
        )

        // 4. Highlights Tab
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.HIGHLIGHTS,
            onClick = { onTabSelected(BottomNavTab.HIGHLIGHTS) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == BottomNavTab.HIGHLIGHTS) Icons.Filled.Movie else Icons.Outlined.Movie,
                    contentDescription = "Highlights"
                )
            },
            label = {
                Text(
                    text = "Highlights",
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == BottomNavTab.HIGHLIGHTS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_highlights")
        )

        // 5. Watchlist Tab
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.SAVED,
            onClick = { onTabSelected(BottomNavTab.SAVED) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == BottomNavTab.SAVED) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Watchlist"
                )
            },
            label = {
                Text(
                    text = "Watchlist",
                    fontSize = 10.sp,
                    fontWeight = if (selectedTab == BottomNavTab.SAVED) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("tab_watchlist")
        )
    }
}
