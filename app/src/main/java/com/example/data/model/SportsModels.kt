package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FirebaseEventDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cat") val cat: String? = null,
    @Json(name = "league_logo") val leagueLogo: String? = null,
    @Json(name = "eventInfo") val eventInfo: FirebaseEventInfoDto? = null,
    @Json(name = "channels_data") val channelsData: List<FirebaseChannelDto>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class FirebaseEventInfoDto(
    @Json(name = "Status") val status: String? = null,
    @Json(name = "eventName") val eventName: String? = null,
    @Json(name = "startTime") val startTime: String? = null,
    @Json(name = "endTime") val endTime: String? = null,
    @Json(name = "teamA") val teamA: String? = null,
    @Json(name = "teamB") val teamB: String? = null,
    @Json(name = "teamAFlag") val teamAFlag: String? = null,
    @Json(name = "teamBFlag") val teamBFlag: String? = null,
    @Json(name = "isHot") val isHot: String? = null
)

@JsonClass(generateAdapter = true)
data class FirebaseChannelDto(
    @Json(name = "title") val title: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "tokenApi") val tokenApi: String? = null
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
    val startEpochMs: Long = 0L,
    val streams: List<StreamItem>,
    val isFavorite: Boolean = false
)

data class StreamItem(
    val channelName: String,
    val fullUrl: String,
    val cleanUrl: String,
    val isIframeOrWeb: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val drmKey: String? = null
)

data class ChannelItem(
    val id: String,
    val title: String,
    val category: String,
    val logoUrl: String,
    val streamUrl: String,
    val isTapmad: Boolean = false,
    val isFavorite: Boolean = false
)

data class HighlightItem(
    val id: String,
    val title: String,
    val category: String,
    val tournament: String,
    val thumbnailUrl: String,
    val dateString: String,
    val servers: List<HighlightServer>,
    val isFavorite: Boolean = false
)

data class HighlightServer(
    val serverName: String,
    val type: String, // "iframe", "direct", "mp4"
    val streamUrl: String
)
