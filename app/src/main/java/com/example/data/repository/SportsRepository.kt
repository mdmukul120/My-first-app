package com.example.data.repository

import com.example.data.api.SportsApiService
import com.example.data.local.FavoriteMatchDao
import com.example.data.local.FavoriteMatchEntity
import com.example.data.model.MatchDto
import com.example.data.model.MatchItem
import com.example.data.model.MatchStatus
import com.example.data.model.SportsFeedResponse
import com.example.data.model.StreamDto
import com.example.data.model.StreamItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

class SportsRepository(
    private val apiService: SportsApiService = SportsApiService.create(),
    private val favoriteMatchDao: FavoriteMatchDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val streamListType = Types.newParameterizedType(List::class.java, StreamDto::class.java)
    private val streamListAdapter = moshi.adapter<List<StreamDto>>(streamListType)

    val favoriteIdsFlow: Flow<Set<String>> = favoriteMatchDao.getFavoriteIds().map { it.toSet() }

    val favoriteMatchesFlow: Flow<List<MatchItem>> = favoriteMatchDao.getAllFavorites().map { entities ->
        entities.map { entity ->
            val streamDtos: List<StreamDto> = try {
                streamListAdapter.fromJson(entity.streamsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            MatchItem(
                id = entity.matchId,
                status = parseStatus(entity.status),
                category = entity.category,
                matchTitle = entity.eventName,
                tournamentName = entity.tournamentName,
                tournamentLogo = entity.tournamentLogo,
                teamAName = entity.teamA,
                teamAFlag = entity.teamAFlag,
                teamBName = entity.teamB,
                teamBFlag = entity.teamBFlag,
                startTimeRaw = entity.startTime,
                streams = streamDtos.map { mapStream(it) },
                isFavorite = true
            )
        }
    }

    suspend fun fetchSportsData(): Result<SportsFeedResponse> {
        return try {
            val response = apiService.getSportsData()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun mapToMatchItems(dtos: List<MatchDto>?, favoriteIds: Set<String>): List<MatchItem> {
        if (dtos == null) return emptyList()
        return dtos.map { dto ->
            val info = dto.eventInfo
            val teamA = info?.teamA?.trim().orEmpty()
            val teamB = info?.teamB?.trim().orEmpty()
            val eventTitle = dto.eventName?.trim() ?: if (teamA.isNotEmpty() && teamB.isNotEmpty()) "$teamA vs $teamB" else "Sports Match"
            val tournament = info?.tournamentName?.trim() ?: "Championship"
            val category = dto.category?.trim() ?: "Other"
            val startTime = info?.startTime?.trim().orEmpty()
            val status = parseStatus(dto.status)

            val matchId = generateMatchId(category, tournament, teamA, teamB, startTime, eventTitle)

            val streams = dto.streams?.map { mapStream(it) } ?: emptyList()

            MatchItem(
                id = matchId,
                status = status,
                category = category,
                matchTitle = eventTitle,
                tournamentName = tournament,
                tournamentLogo = info?.tournamentLogo.orEmpty(),
                teamAName = if (teamA.isNotEmpty()) teamA else "Team 1",
                teamAFlag = info?.teamAFlag.orEmpty(),
                teamBName = if (teamB.isNotEmpty()) teamB else "Team 2",
                teamBFlag = info?.teamBFlag.orEmpty(),
                startTimeRaw = startTime,
                streams = streams,
                isFavorite = favoriteIds.contains(matchId)
            )
        }
    }

    suspend fun toggleFavorite(match: MatchItem) {
        if (match.isFavorite) {
            favoriteMatchDao.deleteFavorite(match.id)
        } else {
            val streamDtos = match.streams.map {
                StreamDto(
                    channelName = it.channelName,
                    streamUrl = it.fullUrl,
                    drmKey = it.drmKey
                )
            }
            val streamsJson = streamListAdapter.toJson(streamDtos)

            val entity = FavoriteMatchEntity(
                matchId = match.id,
                eventName = match.matchTitle,
                tournamentName = match.tournamentName,
                category = match.category,
                teamA = match.teamAName,
                teamB = match.teamBName,
                teamAFlag = match.teamAFlag,
                teamBFlag = match.teamBFlag,
                tournamentLogo = match.tournamentLogo,
                startTime = match.startTimeRaw,
                status = match.status.name,
                streamsJson = streamsJson
            )
            favoriteMatchDao.insertFavorite(entity)
        }
    }

    private fun parseStatus(statusStr: String?): MatchStatus {
        return when (statusStr?.trim()?.uppercase()) {
            "LIVE" -> MatchStatus.LIVE
            "UPCOMING" -> MatchStatus.UPCOMING
            "ENDED" -> MatchStatus.ENDED
            else -> MatchStatus.UPCOMING
        }
    }

    private fun mapStream(dto: StreamDto): StreamItem {
        val fullUrl = dto.streamUrl?.trim().orEmpty()
        val (cleanUrl, headers) = parseStreamUrlAndHeaders(fullUrl)
        return StreamItem(
            channelName = dto.channelName?.trim() ?: "Live Stream Channel",
            fullUrl = fullUrl,
            cleanUrl = cleanUrl,
            headers = headers,
            drmKey = dto.drmKey?.takeIf { it.isNotBlank() }
        )
    }

    private fun parseStreamUrlAndHeaders(url: String): Pair<String, Map<String, String>> {
        if (!url.contains("|")) {
            return Pair(url, emptyMap())
        }
        val parts = url.split("|", limit = 2)
        val cleanUrl = parts[0].trim()
        val headerString = parts[1].trim()
        val headers = mutableMapOf<String, String>()

        headerString.split("&").forEach { param ->
            val kv = param.split("=", limit = 2)
            if (kv.size == 2) {
                headers[kv[0].trim()] = kv[1].trim()
            }
        }
        return Pair(cleanUrl, headers)
    }

    private fun generateMatchId(
        category: String,
        tournament: String,
        teamA: String,
        teamB: String,
        startTime: String,
        eventTitle: String
    ): String {
        val raw = "$category|$tournament|$teamA|$teamB|$startTime|$eventTitle"
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(raw.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            raw.replace("[^a-zA-Z0-9]".toRegex(), "_")
        }
    }
}
