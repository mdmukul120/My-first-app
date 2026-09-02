package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
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
import com.example.data.model.ChannelItem
import com.example.data.model.HighlightItem
import com.example.data.model.HighlightServer
import com.example.data.model.MatchItem
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.ChannelCard
import com.example.ui.components.HighlightCard
import com.example.ui.components.MatchCard
import com.example.ui.components.MatchDetailSheet
import com.example.ui.components.MukulBottomNavBar
import com.example.ui.components.MukulSportsSplash
import com.example.ui.components.StreamPlayerDialog
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SportsCyan
import com.example.ui.theme.SportsOrange
import com.example.ui.theme.TrophyGold
import com.example.ui.viewmodel.BottomNavTab
import com.example.ui.viewmodel.SportsSubTab
import com.example.ui.viewmodel.SportsUiState
import com.example.ui.viewmodel.SportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportsApp(
    viewModel: SportsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (uiState.isInitialSplash) {
        MukulSportsSplash()
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                MukulTopBar(
                    uiState = uiState,
                    onSearchToggle = { viewModel.toggleSearch(!uiState.isSearchActive) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onRefresh = { viewModel.loadAllSportsData(isRefresh = true) }
                )
            },
            bottomBar = {
                MukulBottomNavBar(
                    selectedTab = uiState.currentTab,
                    onTabSelected = { viewModel.selectBottomTab(it) },
                    liveMatchCount = uiState.liveCount
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = uiState.currentTab, label = "tab_content") { tab ->
                    when (tab) {
                        BottomNavTab.LIVE_SPORTS -> LiveSportsScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        BottomNavTab.TAPMAD -> TapmadScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        BottomNavTab.LIVE_TV -> LiveTvScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        BottomNavTab.HIGHLIGHTS -> HighlightsScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        BottomNavTab.SAVED -> SavedScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
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
                onToggleFavorite = { viewModel.toggleFavoriteMatch(match) },
                onSelectStreamForPlayback = { stream ->
                    viewModel.closeMatchDetails()
                    viewModel.playMatchStream(match, stream)
                }
            )
        }

        // Embedded In-App Video Player Dialog
        if (uiState.isPlayerOpen && uiState.currentPlayingStream != null) {
            StreamPlayerDialog(
                title = uiState.playerTitle,
                subtitle = uiState.playerSubtitle,
                currentStream = uiState.currentPlayingStream!!,
                allStreams = uiState.playerStreamsList,
                onSelectStream = { stream ->
                    viewModel.switchPlayingStream(stream)
                },
                onDismiss = { viewModel.closePlayer() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSportsScreen(
    uiState: SportsUiState,
    viewModel: SportsViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // SubTabs: All, Live Now, Upcoming
        PrimaryTabRow(
            selectedTabIndex = uiState.sportsSubTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("sports_sub_tabs")
        ) {
            SportsSubTab.values().forEach { subTab ->
                val isSelected = uiState.sportsSubTab == subTab
                val label = when (subTab) {
                    SportsSubTab.ALL -> "All Matches"
                    SportsSubTab.LIVE_NOW -> "Live Now (${uiState.liveCount})"
                    SportsSubTab.UPCOMING -> "Upcoming (${uiState.upcomingCount})"
                }
                Tab(
                    selected = isSelected,
                    onClick = { viewModel.selectSportsSubTab(subTab) },
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (subTab == SportsSubTab.LIVE_NOW) LiveRed else MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                )
            }
        }

        // Category Filter
        CategoryFilterRow(
            categories = uiState.sportsCategories,
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) }
        )

        // Matches List
        val matches = uiState.filteredLiveMatches
        if (uiState.isLoading && !uiState.isRefreshing) {
            LoadingSpinner()
        } else if (matches.isEmpty()) {
            EmptyView(
                icon = Icons.Default.SportsSoccer,
                title = "No Live or Upcoming Matches",
                desc = "No active fixtures found in this category right now. Past ended matches have been hidden.",
                actionLabel = "Show All Categories",
                onAction = { viewModel.selectCategory("ALL") }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("live_matches_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(matches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        currentEpochMs = uiState.currentEpochMs,
                        onClick = { viewModel.openMatchDetails(match) },
                        onToggleFavorite = { viewModel.toggleFavoriteMatch(match) }
                    )
                }
            }
        }
    }
}

@Composable
fun TapmadScreen(
    uiState: SportsUiState,
    viewModel: SportsViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryFilterRow(
            categories = uiState.tapmadCategories,
            selectedCategory = uiState.selectedTapmadCategory,
            onCategorySelected = { viewModel.selectTapmadCategory(it) }
        )

        val channels = uiState.filteredTapmadChannels
        if (uiState.isLoading && !uiState.isRefreshing) {
            LoadingSpinner()
        } else if (channels.isEmpty()) {
            EmptyView(
                icon = Icons.Default.LiveTv,
                title = "No Tapmad Channels Found",
                desc = "Could not find any active Tapmad sports channels for the current search.",
                actionLabel = "Clear Filter",
                onAction = { viewModel.selectTapmadCategory("ALL") }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tapmad_channel_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { viewModel.playChannel(channel) },
                        onToggleFavorite = { viewModel.toggleFavoriteChannel(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun LiveTvScreen(
    uiState: SportsUiState,
    viewModel: SportsViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryFilterRow(
            categories = uiState.liveTvCategories,
            selectedCategory = uiState.selectedLiveTvCategory,
            onCategorySelected = { viewModel.selectLiveTvCategory(it) }
        )

        val channels = uiState.filteredLiveTvChannels
        if (uiState.isLoading && !uiState.isRefreshing) {
            LoadingSpinner()
        } else if (channels.isEmpty()) {
            EmptyView(
                icon = Icons.Default.Tv,
                title = "No TV Channels Found",
                desc = "No channels match your current selection.",
                actionLabel = "Show All TV",
                onAction = { viewModel.selectLiveTvCategory("ALL") }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("live_tv_channel_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(channels, key = { it.id }) { channel ->
                    ChannelCard(
                        channel = channel,
                        onClick = { viewModel.playChannel(channel) },
                        onToggleFavorite = { viewModel.toggleFavoriteChannel(channel) }
                    )
                }
            }
        }
    }
}

@Composable
fun HighlightsScreen(
    uiState: SportsUiState,
    viewModel: SportsViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryFilterRow(
            categories = uiState.highlightCategories,
            selectedCategory = uiState.selectedHighlightCategory,
            onCategorySelected = { viewModel.selectHighlightCategory(it) }
        )

        val list = uiState.filteredHighlights
        if (uiState.isLoading && !uiState.isRefreshing) {
            LoadingSpinner()
        } else if (list.isEmpty()) {
            EmptyView(
                icon = Icons.Default.Movie,
                title = "No Highlights Found",
                desc = "Match replays will appear here once loaded.",
                actionLabel = "Show All",
                onAction = { viewModel.selectHighlightCategory("ALL") }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("highlights_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { highlight ->
                    HighlightCard(
                        highlight = highlight,
                        onSelectServer = { server ->
                            viewModel.playHighlight(highlight, server)
                        },
                        onToggleFavorite = { viewModel.toggleFavoriteHighlight(highlight) }
                    )
                }
            }
        }
    }
}

@Composable
fun SavedScreen(
    uiState: SportsUiState,
    viewModel: SportsViewModel
) {
    val favorites = uiState.favoriteMatches
    if (favorites.isEmpty()) {
        EmptyView(
            icon = Icons.Default.Bookmark,
            title = "Watchlist is Empty",
            desc = "Bookmark your favorite live matches, Tapmad streams, or TV channels to access them quickly here.",
            actionLabel = null,
            onAction = {}
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("watchlist_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(favorites, key = { it.id }) { match ->
                MatchCard(
                    match = match,
                    currentEpochMs = uiState.currentEpochMs,
                    onClick = { viewModel.openMatchDetails(match) },
                    onToggleFavorite = { viewModel.toggleFavoriteMatch(match) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MukulTopBar(
    uiState: SportsUiState,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            if (uiState.isSearchActive) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search matches, channels, teams...") },
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
                    Image(
                        painter = painterResource(id = R.drawable.mukul_sports_logo_1788310180221),
                        contentDescription = "Mukul Sports",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mukul Sports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (uiState.liveCount > 0) {
                            Text(
                                text = "🔴 ${uiState.liveCount} Live Matches",
                                style = MaterialTheme.typography.labelSmall,
                                color = LiveRed,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Live Sports & IPTV",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun LoadingSpinner() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Loading sports feeds...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    actionLabel: String?,
    onAction: () -> Unit
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}
