package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ChannelItem
import com.example.data.model.HighlightItem
import com.example.data.model.HighlightServer
import com.example.data.model.MatchItem
import com.example.data.model.MatchStatus
import com.example.data.model.StreamItem
import com.example.data.repository.SportsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BottomNavTab {
    LIVE_SPORTS,
    TAPMAD,
    LIVE_TV,
    HIGHLIGHTS,
    SAVED
}

enum class SportsSubTab {
    ALL,
    LIVE_NOW,
    UPCOMING
}

data class SportsUiState(
    val isInitialSplash: Boolean = true,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentEpochMs: Long = System.currentTimeMillis(),
    val currentTab: BottomNavTab = BottomNavTab.LIVE_SPORTS,
    val sportsSubTab: SportsSubTab = SportsSubTab.ALL,

    // Live Sports Data
    val liveMatches: List<MatchItem> = emptyList(),
    val sportsCategories: List<String> = listOf("ALL", "Football", "Cricket", "Tennis", "Motorsport", "Basketball", "Baseball", "Other"),
    val selectedCategory: String = "ALL",

    // Tapmad Channels
    val tapmadChannels: List<ChannelItem> = emptyList(),
    val tapmadCategories: List<String> = listOf("ALL", "Cricket", "Tennis", "Football", "General"),
    val selectedTapmadCategory: String = "ALL",

    // Live TV Channels
    val bdipChannels: List<ChannelItem> = emptyList(),
    val liveTvCategories: List<String> = listOf("ALL", "Sports", "Bangla", "Entertainment", "News", "Movies"),
    val selectedLiveTvCategory: String = "ALL",

    // Highlights & Replays
    val highlights: List<HighlightItem> = emptyList(),
    val highlightCategories: List<String> = listOf("ALL", "Football", "Motorsport", "Baseball", "Rugby", "Basketball"),
    val selectedHighlightCategory: String = "ALL",

    // Saved Favorites
    val favoriteMatches: List<MatchItem> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),

    // Search
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,

    // Dialogs & Player
    val selectedMatchForDetails: MatchItem? = null,
    val isPlayerOpen: Boolean = false,
    val playerTitle: String = "",
    val playerSubtitle: String = "",
    val currentPlayingStream: StreamItem? = null,
    val playerStreamsList: List<StreamItem> = emptyList(),

    val isCommunityDialogOpen: Boolean = false
) {
    val liveCount: Int get() = liveMatches.count { it.status == MatchStatus.LIVE }
    val upcomingCount: Int get() = liveMatches.count { it.status == MatchStatus.UPCOMING }

    val filteredLiveMatches: List<MatchItem>
        get() {
            var list = when (sportsSubTab) {
                SportsSubTab.ALL -> liveMatches
                SportsSubTab.LIVE_NOW -> liveMatches.filter { it.status == MatchStatus.LIVE }
                SportsSubTab.UPCOMING -> liveMatches.filter { it.status == MatchStatus.UPCOMING }
            }

            if (!selectedCategory.equals("ALL", ignoreCase = true)) {
                list = list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.matchTitle.lowercase().contains(q) ||
                            it.teamAName.lowercase().contains(q) ||
                            it.teamBName.lowercase().contains(q) ||
                            it.tournamentName.lowercase().contains(q) ||
                            it.category.lowercase().contains(q)
                }
            }
            return list
        }

    val filteredTapmadChannels: List<ChannelItem>
        get() {
            var list = tapmadChannels
            if (!selectedTapmadCategory.equals("ALL", ignoreCase = true)) {
                list = list.filter { it.category.contains(selectedTapmadCategory, ignoreCase = true) }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) }
            }
            return list
        }

    val filteredLiveTvChannels: List<ChannelItem>
        get() {
            var list = bdipChannels
            if (!selectedLiveTvCategory.equals("ALL", ignoreCase = true)) {
                list = list.filter { it.category.contains(selectedLiveTvCategory, ignoreCase = true) }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) }
            }
            return list
        }

    val filteredHighlights: List<HighlightItem>
        get() {
            var list = highlights
            if (!selectedHighlightCategory.equals("ALL", ignoreCase = true)) {
                list = list.filter { it.category.equals(selectedHighlightCategory, ignoreCase = true) }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                            it.tournament.lowercase().contains(q) ||
                            it.category.lowercase().contains(q)
                }
            }
            return list
        }
}

class SportsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = SportsRepository(favoriteMatchDao = database.favoriteMatchDao())

    private val _uiState = MutableStateFlow(SportsUiState())
    val uiState: StateFlow<SportsUiState> = _uiState.asStateFlow()

    init {
        // Collect favorites
        viewModelScope.launch {
            repository.favoriteIdsFlow.collect { favIds ->
                _uiState.update { state ->
                    state.copy(
                        favoriteIds = favIds,
                        liveMatches = state.liveMatches.map { it.copy(isFavorite = favIds.contains(it.id)) },
                        tapmadChannels = state.tapmadChannels.map { it.copy(isFavorite = favIds.contains(it.id)) },
                        bdipChannels = state.bdipChannels.map { it.copy(isFavorite = favIds.contains(it.id)) },
                        highlights = state.highlights.map { it.copy(isFavorite = favIds.contains(it.id)) }
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.favoriteMatchesFlow.collect { favMatches ->
                _uiState.update { it.copy(favoriteMatches = favMatches) }
            }
        }

        // Realtime Clock Ticker for Live Match Countdown
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(currentEpochMs = System.currentTimeMillis()) }
            }
        }

        // Initial Data Load & Splash Animation dismissal
        loadAllSportsData()
    }

    fun loadAllSportsData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                else it.copy(isLoading = true, errorMessage = null)
            }

            val favIds = _uiState.value.favoriteIds

            // Fetch live sports events
            val liveResult = repository.fetchLiveSportsEvents(favIds)
            // Fetch tapmad channels
            val tapmadResult = repository.fetchTapmadChannels(favIds)
            // Fetch BDIP TV channels
            val bdipResult = repository.fetchBdipTvChannels(favIds)
            // Fetch highlights
            val highlightsResult = repository.fetchHighlights(favIds)

            val matches = liveResult.getOrDefault(emptyList())
            val tapmadList = tapmadResult.getOrDefault(emptyList())
            val bdipList = bdipResult.getOrDefault(emptyList())
            val highlightList = highlightsResult.getOrDefault(emptyList())

            // Dynamically collect sports categories
            val dynamicSportsCategories = mutableListOf("ALL")
            val sportsCats = matches.map { it.category }.filter { it.isNotBlank() }.distinct()
            dynamicSportsCategories.addAll(sportsCats)

            // Dynamically collect live tv categories
            val dynamicTvCategories = mutableListOf("ALL", "Sports")
            val tvCats = bdipList.map { it.category }
                .map { it.replace("Bangladesh:", "").trim() }
                .filter { it.isNotBlank() && !it.equals("Sports", ignoreCase = true) }
                .distinct()
            dynamicTvCategories.addAll(tvCats)

            val dynamicHighlightCats = mutableListOf("ALL")
            val highCats = highlightList.map { it.category }.filter { it.isNotBlank() }.distinct()
            dynamicHighlightCats.addAll(highCats)

            // If this was initial load, show animation briefly for brand polish
            if (_uiState.value.isInitialSplash) {
                delay(1200)
            }

            _uiState.update { state ->
                state.copy(
                    isInitialSplash = false,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = if (matches.isEmpty() && tapmadList.isEmpty() && bdipList.isEmpty()) {
                        "Unable to load sports feeds. Please check internet connection."
                    } else null,
                    liveMatches = matches,
                    tapmadChannels = tapmadList,
                    bdipChannels = bdipList,
                    highlights = highlightList,
                    sportsCategories = if (dynamicSportsCategories.size > 1) dynamicSportsCategories else state.sportsCategories,
                    liveTvCategories = if (dynamicTvCategories.size > 1) dynamicTvCategories else state.liveTvCategories,
                    highlightCategories = if (dynamicHighlightCats.size > 1) dynamicHighlightCats else state.highlightCategories
                )
            }
        }
    }

    fun dismissSplash() {
        _uiState.update { it.copy(isInitialSplash = false) }
    }

    fun selectBottomTab(tab: BottomNavTab) {
        _uiState.update { it.copy(currentTab = tab, searchQuery = "", isSearchActive = false) }
    }

    fun selectSportsSubTab(subTab: SportsSubTab) {
        _uiState.update { it.copy(sportsSubTab = subTab) }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectTapmadCategory(category: String) {
        _uiState.update { it.copy(selectedTapmadCategory = category) }
    }

    fun selectLiveTvCategory(category: String) {
        _uiState.update { it.copy(selectedLiveTvCategory = category) }
    }

    fun selectHighlightCategory(category: String) {
        _uiState.update { it.copy(selectedHighlightCategory = category) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSearch(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
    }

    fun toggleFavoriteMatch(match: MatchItem) {
        viewModelScope.launch {
            repository.toggleFavoriteMatch(match)
        }
    }

    fun toggleFavoriteChannel(channel: ChannelItem) {
        viewModelScope.launch {
            repository.toggleFavoriteChannel(channel)
        }
    }

    fun toggleFavoriteHighlight(highlight: HighlightItem) {
        viewModelScope.launch {
            repository.toggleFavoriteHighlight(highlight)
        }
    }

    fun openMatchDetails(match: MatchItem) {
        _uiState.update { it.copy(selectedMatchForDetails = match) }
    }

    fun closeMatchDetails() {
        _uiState.update { it.copy(selectedMatchForDetails = null) }
    }

    // Direct playback for sports match stream
    fun playMatchStream(match: MatchItem, stream: StreamItem) {
        _uiState.update {
            it.copy(
                isPlayerOpen = true,
                playerTitle = match.matchTitle,
                playerSubtitle = "${match.tournamentName} • ${stream.channelName}",
                currentPlayingStream = stream,
                playerStreamsList = match.streams
            )
        }
    }

    // Direct playback for Tapmad or Live TV channel
    fun playChannel(channel: ChannelItem) {
        val stream = StreamItem(
            channelName = channel.title,
            fullUrl = channel.streamUrl,
            cleanUrl = channel.streamUrl
        )
        _uiState.update {
            it.copy(
                isPlayerOpen = true,
                playerTitle = channel.title,
                playerSubtitle = "${channel.category} • Live Broadcast",
                currentPlayingStream = stream,
                playerStreamsList = listOf(stream)
            )
        }
    }

    // Direct playback for Highlight / Replay server
    fun playHighlight(highlight: HighlightItem, server: HighlightServer) {
        val stream = StreamItem(
            channelName = server.serverName,
            fullUrl = server.streamUrl,
            cleanUrl = server.streamUrl,
            isIframeOrWeb = server.type.contains("iframe", ignoreCase = true) || !server.streamUrl.contains(".m3u8")
        )
        val allStreams = highlight.servers.map {
            StreamItem(
                channelName = it.serverName,
                fullUrl = it.streamUrl,
                cleanUrl = it.streamUrl,
                isIframeOrWeb = it.type.contains("iframe", ignoreCase = true) || !it.streamUrl.contains(".m3u8")
            )
        }
        _uiState.update {
            it.copy(
                isPlayerOpen = true,
                playerTitle = highlight.title,
                playerSubtitle = "${highlight.tournament} • ${server.serverName}",
                currentPlayingStream = stream,
                playerStreamsList = allStreams
            )
        }
    }

    fun switchPlayingStream(stream: StreamItem) {
        _uiState.update {
            it.copy(
                currentPlayingStream = stream,
                playerSubtitle = it.playerSubtitle.substringBeforeLast("•") + "• " + stream.channelName
            )
        }
    }

    fun closePlayer() {
        _uiState.update {
            it.copy(
                isPlayerOpen = false,
                currentPlayingStream = null,
                playerStreamsList = emptyList()
            )
        }
    }

    fun setCommunityDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isCommunityDialogOpen = open) }
    }
}
