package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SportsFeedResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "owner") val owner: String? = null,
    @Json(name = "telegram_channel") val telegramChannel: String? = null,
    @Json(name = "last_update_time") val lastUpdateTime: String? = null,
    @Json(name = "total_matches") val totalMatches: Int? = 0,
    @Json(name = "live_match") val liveMatchesCount: Int? = 0,
    @Json(name = "matches") val matches: List<MatchDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class MatchDto(
    @Json(name = "status") val status: String? = null,
    @Json(name = "Category") val category: String? = null,
    @Json(name = "event_name") val eventName: String? = null,
    @Json(name = "eventInfo") val eventInfo: EventInfoDto? = null,
    @Json(name = "streams") val streams: List<StreamDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class EventInfoDto(
    @Json(name = "teamA") val teamA: String? = null,
    @Json(name = "teamB") val teamB: String? = null,
    @Json(name = "teamAFlag") val teamAFlag: String? = null,
    @Json(name = "teamBFlag") val teamBFlag: String? = null,
    @Json(name = "eventName") val tournamentName: String? = null,
    @Json(name = "event_logo") val tournamentLogo: String? = null,
    @Json(name = "startTime") val startTime: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamDto(
    @Json(name = "channel_name") val channelName: String? = null,
    @Json(name = "stream_url") val streamUrl: String? = null,
    @Json(name = "drm_key") val drmKey: String? = null
)

enum class MatchStatus {
    LIVE,
    UPCOMING,
    ENDED
}

data class MatchItem(
    val id: String,
    val status: MatchStatus,
    val category: String,
    val matchTitle: String,
    val tournamentName: String,
    val tournamentLogo: String,
    val teamAName: String,
    val teamAFlag: String,
    val teamBName: String,
    val teamBFlag: String,
    val startTimeRaw: String,
    val streams: List<StreamItem>,
    val isFavorite: Boolean = false
)

data class StreamItem(
    val channelName: String,
    val fullUrl: String,
    val cleanUrl: String,
    val headers: Map<String, String>,
    val drmKey: String? = null
)
