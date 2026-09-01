package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.MatchDto
import com.example.data.model.MatchItem
import com.example.data.model.MatchStatus
import com.example.data.model.StreamItem
import com.example.data.repository.SportsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StatusTab {
    LIVE,
    UPCOMING,
    ENDED,
    FAVORITES
}

data class SportsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val feedTitle: String = "Live & Upcoming Sports",
    val owner: String = "",
    val telegramChannel: String = "https://t.me/sm_iptv_bd",
    val lastUpdateTime: String = "",
    val totalMatches: Int = 0,
    val rawMatches: List<MatchDto> = emptyList(),
    val allMatches: List<MatchItem> = emptyList(),
    val favoriteMatches: List<MatchItem> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val categories: List<String> = listOf("ALL", "Football", "Cricket", "Boxing", "Motorsports", "Baseball", "Other"),
    val selectedCategory: String = "ALL",
    val selectedTab: StatusTab = StatusTab.LIVE,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedMatchForDetails: MatchItem? = null,
    val selectedStreamForPlayback: StreamItem? = null,
    val activeMatchForPlayback: MatchItem? = null,
    val isCommunityDialogOpen: Boolean = false
) {
    val liveCount: Int get() = allMatches.count { it.status == MatchStatus.LIVE }
    val upcomingCount: Int get() = allMatches.count { it.status == MatchStatus.UPCOMING }
    val endedCount: Int get() = allMatches.count { it.status == MatchStatus.ENDED }
    val favoriteCount: Int get() = favoriteMatches.size

    val filteredMatches: List<MatchItem>
        get() {
            val baseList = when (selectedTab) {
                StatusTab.LIVE -> allMatches.filter { it.status == MatchStatus.LIVE }
                StatusTab.UPCOMING -> allMatches.filter { it.status == MatchStatus.UPCOMING }
                StatusTab.ENDED -> allMatches.filter { it.status == MatchStatus.ENDED }
                StatusTab.FAVORITES -> favoriteMatches
            }

            val categoryFiltered = if (selectedCategory.equals("ALL", ignoreCase = true)) {
                baseList
            } else {
                baseList.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }

            return if (searchQuery.isBlank()) {
                categoryFiltered
            } else {
                val q = searchQuery.trim().lowercase()
                categoryFiltered.filter { match ->
                    match.matchTitle.lowercase().contains(q) ||
                            match.teamAName.lowercase().contains(q) ||
                            match.teamBName.lowercase().contains(q) ||
                            match.tournamentName.lowercase().contains(q) ||
                            match.category.lowercase().contains(q)
                }
            }
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
                    val updatedAll = repository.mapToMatchItems(state.rawMatches, favIds)
                    state.copy(
                        favoriteIds = favIds,
                        allMatches = updatedAll
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.favoriteMatchesFlow.collect { favMatches ->
                _uiState.update { it.copy(favoriteMatches = favMatches) }
            }
        }

        loadSportsData()
    }

    fun loadSportsData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, errorMessage = null)
                else it.copy(isLoading = true, errorMessage = null)
            }

            val result = repository.fetchSportsData()
            result.onSuccess { response ->
                val rawDtos = response.matches ?: emptyList()
                val currentFavIds = _uiState.value.favoriteIds
                val mappedMatches = repository.mapToMatchItems(rawDtos, currentFavIds)

                // Extract available categories dynamically
                val dynamicCategories = mutableListOf("ALL")
                val foundCategories = rawDtos.mapNotNull { it.category?.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                dynamicCategories.addAll(foundCategories)

                // Determine default tab: if live matches exist, show LIVE, else UPCOMING
                val hasLive = mappedMatches.any { it.status == MatchStatus.LIVE }
                val initialTab = if (hasLive) StatusTab.LIVE else StatusTab.UPCOMING

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = null,
                        feedTitle = response.name?.ifBlank { "Live Sports" } ?: "Live Sports",
                        owner = response.owner.orEmpty(),
                        telegramChannel = response.telegramChannel?.ifBlank { "https://t.me/sm_iptv_bd" } ?: "https://t.me/sm_iptv_bd",
                        lastUpdateTime = response.lastUpdateTime.orEmpty(),
                        totalMatches = response.totalMatches ?: mappedMatches.size,
                        rawMatches = rawDtos,
                        allMatches = mappedMatches,
                        categories = if (dynamicCategories.size > 1) dynamicCategories else state.categories,
                        selectedTab = if (state.isLoading) initialTab else state.selectedTab
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.localizedMessage ?: "Failed to fetch live sports data. Please check internet connection."
                    )
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun selectTab(tab: StatusTab) {
        _uiState.update { it.copy(selectedTab = tab) }
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

    fun toggleFavorite(match: MatchItem) {
        viewModelScope.launch {
            repository.toggleFavorite(match)
        }
    }

    fun openMatchDetails(match: MatchItem) {
        _uiState.update { it.copy(selectedMatchForDetails = match) }
    }

    fun closeMatchDetails() {
        _uiState.update { it.copy(selectedMatchForDetails = null) }
    }

    fun playStream(match: MatchItem, stream: StreamItem) {
        _uiState.update {
            it.copy(
                activeMatchForPlayback = match,
                selectedStreamForPlayback = stream
            )
        }
    }

    fun closePlayback() {
        _uiState.update {
            it.copy(
                activeMatchForPlayback = null,
                selectedStreamForPlayback = null
            )
        }
    }

    fun setCommunityDialogOpen(open: Boolean) {
        _uiState.update { it.copy(isCommunityDialogOpen = open) }
    }
}
