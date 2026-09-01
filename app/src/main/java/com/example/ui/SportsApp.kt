package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.MatchStatus
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.CommunityDialog
import com.example.ui.components.MatchCard
import com.example.ui.components.MatchDetailSheet
import com.example.ui.components.StreamPlayerDialog
import com.example.ui.theme.LiveRed
import com.example.ui.theme.PitchGreen
import com.example.ui.theme.SportsCyan
import com.example.ui.theme.SportsOrange
import com.example.ui.theme.TrophyGold
import com.example.ui.viewmodel.SportsUiState
import com.example.ui.viewmodel.SportsViewModel
import com.example.ui.viewmodel.StatusTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsApp(
    viewModel: SportsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SportsTopBar(
                uiState = uiState,
                onSearchToggle = { viewModel.toggleSearch(!uiState.isSearchActive) },
                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                onRefresh = { viewModel.loadSportsData(isRefresh = true) },
                onOpenCommunity = { viewModel.setCommunityDialogOpen(true) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Status Tabs (LIVE, UPCOMING, ENDED, FAVORITES)
            StatusTabsRow(
                selectedTab = uiState.selectedTab,
                liveCount = uiState.liveCount,
                upcomingCount = uiState.upcomingCount,
                endedCount = uiState.endedCount,
                favoriteCount = uiState.favoriteCount,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // Category Filter Row
            CategoryFilterRow(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading && !uiState.isRefreshing -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading sports matches & streams...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    uiState.errorMessage != null && uiState.allMatches.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Could not connect to sports feed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.errorMessage ?: "Please verify your internet connection.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadSportsData() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("retry_button")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Retry Connection")
                                }
                            }
                        }
                    }

                    else -> {
                        val matches = uiState.filteredMatches
                        if (matches.isEmpty()) {
                            EmptyMatchesState(
                                selectedTab = uiState.selectedTab,
                                selectedCategory = uiState.selectedCategory,
                                searchQuery = uiState.searchQuery,
                                onResetFilter = {
                                    viewModel.selectCategory("ALL")
                                    viewModel.setSearchQuery("")
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("matches_list"),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Match count info banner
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${matches.size} Match${if (matches.size > 1) "es" else ""}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )

                                        if (uiState.lastUpdateTime.isNotBlank()) {
                                            Text(
                                                text = "Synced ${uiState.lastUpdateTime}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                items(
                                    items = matches,
                                    key = { it.id }
                                ) { match ->
                                    MatchCard(
                                        match = match,
                                        onCardClick = { viewModel.openMatchDetails(match) },
                                        onToggleFavorite = { viewModel.toggleFavorite(match) },
                                        onQuickWatchClick = if (match.streams.isNotEmpty()) {
                                            {
                                                if (match.streams.size == 1) {
                                                    viewModel.playStream(match, match.streams[0])
                                                } else {
                                                    viewModel.openMatchDetails(match)
                                                }
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Match Details BottomSheet
    uiState.selectedMatchForDetails?.let { match ->
        MatchDetailSheet(
            match = match,
            sheetState = detailSheetState,
            onDismiss = { viewModel.closeMatchDetails() },
            onToggleFavorite = { viewModel.toggleFavorite(match) },
            onSelectStreamForPlayback = { stream ->
                viewModel.playStream(match, stream)
            }
        )
    }

    // Video Stream Playback Dialog
    if (uiState.activeMatchForPlayback != null && uiState.selectedStreamForPlayback != null) {
        StreamPlayerDialog(
            match = uiState.activeMatchForPlayback!!,
            stream = uiState.selectedStreamForPlayback!!,
            onDismiss = { viewModel.closePlayback() }
        )
    }

    // Community Info Dialog
    if (uiState.isCommunityDialogOpen) {
        CommunityDialog(
            feedName = uiState.feedTitle,
            owner = uiState.owner,
            telegramUrl = uiState.telegramChannel,
            lastUpdate = uiState.lastUpdateTime,
            totalMatches = uiState.totalMatches,
            liveMatches = uiState.liveCount,
            onDismiss = { viewModel.setCommunityDialogOpen(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsTopBar(
    uiState: SportsUiState,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenCommunity: () -> Unit
) {
    TopAppBar(
        title = {
            if (uiState.isSearchActive) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search match, team, league...") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field"),
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡",
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Live Sports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.liveCount > 0) {
                            Text(
                                text = "🔴 ${uiState.liveCount} Matches Streaming",
                                style = MaterialTheme.typography.labelSmall,
                                color = LiveRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSearchToggle,
                modifier = Modifier.testTag("search_toggle_btn")
            ) {
                Icon(
                    imageVector = if (uiState.isSearchActive) Icons.Default.Clear else Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_btn")
            ) {
                if (uiState.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            IconButton(
                onClick = onOpenCommunity,
                modifier = Modifier.testTag("community_info_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Feed Info",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusTabsRow(
    selectedTab: StatusTab,
    liveCount: Int,
    upcomingCount: Int,
    endedCount: Int,
    favoriteCount: Int,
    onTabSelected: (StatusTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("status_tabs_row")
    ) {
        StatusTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val (title, count, badgeColor) = when (tab) {
                StatusTab.LIVE -> Triple("LIVE", liveCount, LiveRed)
                StatusTab.UPCOMING -> Triple("UPCOMING", upcomingCount, SportsOrange)
                StatusTab.ENDED -> Triple("ENDED", endedCount, MaterialTheme.colorScheme.onSurfaceVariant)
                StatusTab.FAVORITES -> Triple("SAVED", favoriteCount, TrophyGold)
            }

            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = if (isSelected) {
                                if (tab == StatusTab.LIVE) LiveRed else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (count > 0) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) badgeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) badgeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyMatchesState(
    selectedTab: StatusTab,
    selectedCategory: String,
    searchQuery: String,
    onResetFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (selectedTab) {
                    StatusTab.FAVORITES -> Icons.Default.Bookmark
                    else -> Icons.Default.SportsSoccer
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val title = when {
            searchQuery.isNotBlank() -> "No matching matches found"
            selectedTab == StatusTab.FAVORITES -> "No saved matches yet"
            selectedTab == StatusTab.LIVE -> "No live matches currently"
            selectedTab == StatusTab.UPCOMING -> "No upcoming matches in this category"
            else -> "No matches found"
        }

        val desc = when {
            searchQuery.isNotBlank() -> "Try checking your spelling or search for another team or tournament."
            selectedTab == StatusTab.FAVORITES -> "Tap the bookmark icon on any match card to save it to your watchlist for quick access."
            selectedTab == StatusTab.LIVE -> "There are no matches currently broadcasting in $selectedCategory. Check the Upcoming tab for scheduled fixtures!"
            else -> "Check back soon for new sports events and live broadcast schedules."
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (searchQuery.isNotBlank() || !selectedCategory.equals("ALL", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onResetFilter,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Show All Sports")
            }
        }
    }
}
