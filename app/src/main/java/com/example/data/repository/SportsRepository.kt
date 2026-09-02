package com.example.data.repository

import com.example.data.api.SportsApiService
import com.example.data.local.FavoriteMatchDao
import com.example.data.local.FavoriteMatchEntity
import com.example.data.model.ChannelItem
import com.example.data.model.FirebaseChannelDto
import com.example.data.model.FirebaseEventDto
import com.example.data.model.HighlightItem
import com.example.data.model.HighlightServer
import com.example.data.model.MatchItem
import com.example.data.model.MatchStatus
import com.example.data.model.StreamItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class SportsRepository(
    private val apiService: SportsApiService = SportsApiService(),
    private val favoriteMatchDao: FavoriteMatchDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val streamListType = Types.newParameterizedType(List::class.java, StreamItem::class.java)
    private val streamListAdapter = moshi.adapter<List<StreamItem>>(streamListType)

    val favoriteIdsFlow: Flow<Set<String>> = favoriteMatchDao.getFavoriteIds().map { it.toSet() }

    val favoriteMatchesFlow: Flow<List<MatchItem>> = favoriteMatchDao.getAllFavorites().map { entities ->
        entities.map { entity ->
            val streams: List<StreamItem> = try {
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
                startEpochMs = parseDateToEpochMs(entity.startTime),
                streams = streams,
                isFavorite = true
            )
        }
    }

    suspend fun fetchLiveSportsEvents(favoriteIds: Set<String>): Result<List<MatchItem>> = withContext(Dispatchers.Default) {
        try {
            val dtos = apiService.getLiveSportsEvents()
            val now = System.currentTimeMillis()

            // Map and filter matches:
            // "রিসেন্ট টাইমেইর আগের হয়ে যাওয়া সকল ম্যাচ কোন কিছু দেখাবে না"
            // Hide matches that finished (started more than 3 hours ago)
            val matches = dtos.mapNotNull { dto ->
                mapFirebaseEventToMatchItem(dto, favoriteIds, now)
            }.sortedWith(
                compareBy<MatchItem> { it.status != MatchStatus.LIVE }
                    .thenBy { it.startEpochMs }
            )

            Result.success(matches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTapmadChannels(favoriteIds: Set<String>): Result<List<ChannelItem>> = withContext(Dispatchers.Default) {
        try {
            val text = apiService.getTapmadPlaylist()
            val channels = parseM3uChannels(text, isTapmad = true, favoriteIds = favoriteIds)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchBdipTvChannels(favoriteIds: Set<String>): Result<List<ChannelItem>> = withContext(Dispatchers.Default) {
        try {
            val text = apiService.getBdipTvPlaylist()
            val channels = parseM3uChannels(text, isTapmad = false, favoriteIds = favoriteIds)
            Result.success(channels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHighlights(favoriteIds: Set<String>): Result<List<HighlightItem>> = withContext(Dispatchers.Default) {
        try {
            val text = apiService.getReplaysText()
            val highlights = parseReplays(text, favoriteIds)
            Result.success(highlights)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapFirebaseEventToMatchItem(
        dto: FirebaseEventDto,
        favoriteIds: Set<String>,
        now: Long
    ): MatchItem? {
        val info = dto.eventInfo
        val teamA = info?.teamA?.trim().orEmpty()
        val teamB = info?.teamB?.trim().orEmpty()
        val title = dto.title?.trim() ?: if (teamA.isNotEmpty() && teamB.isNotEmpty()) "$teamA vs $teamB" else "Match"
        val category = dto.cat?.trim() ?: "Football"
        val tournament = info?.eventName?.trim() ?: title
        val startTimeStr = info?.startTime?.trim().orEmpty()
        val startEpochMs = parseDateToEpochMs(startTimeStr)

        // Determine status based on start time & current time
        // If startEpochMs is unknown (0), treat as UPCOMING
        val status = when {
            startEpochMs == 0L -> MatchStatus.UPCOMING
            now < startEpochMs -> MatchStatus.UPCOMING
            now in startEpochMs..(startEpochMs + 3 * 3600 * 1000) -> MatchStatus.LIVE
            else -> MatchStatus.ENDED
        }

        // Rule: HIDE ended matches! ("রিসেন্ট টাইমেইর আগের হয়ে যাওয়া সকল ম্যাচ কোন কিছু দেখাবে না")
        // If match is ended (started > 3 hours ago), filter out completely
        // But if startEpochMs is 0, allow it.
        if (status == MatchStatus.ENDED && startEpochMs > 0L && (now - startEpochMs) > (3 * 3600 * 1000)) {
            return null
        }

        val matchId = generateMatchId(category, tournament, teamA, teamB, startTimeStr, title)

        val streams = dto.channelsData?.mapNotNull { mapChannelDtoToStream(it) } ?: emptyList()

        return MatchItem(
            id = matchId,
            status = status,
            category = category,
            matchTitle = title,
            tournamentName = tournament,
            tournamentLogo = dto.leagueLogo?.trim().orEmpty(),
            teamAName = if (teamA.isNotEmpty()) teamA else title.split("vs").getOrNull(0)?.trim() ?: "Team A",
            teamAFlag = info?.teamAFlag?.trim().orEmpty(),
            teamBName = if (teamB.isNotEmpty()) teamB else title.split("vs").getOrNull(1)?.trim() ?: "Team B",
            teamBFlag = info?.teamBFlag?.trim().orEmpty(),
            startTimeRaw = startTimeStr,
            startEpochMs = startEpochMs,
            streams = streams,
            isFavorite = favoriteIds.contains(matchId)
        )
    }

    private fun mapChannelDtoToStream(dto: FirebaseChannelDto): StreamItem? {
        val rawLink = dto.link?.trim().orEmpty()
        if (rawLink.isBlank()) return null

        val channelTitle = dto.title?.trim() ?: "Stream Channel"
        var cleanLink = rawLink
        var isIframeOrWeb = false

        // Extract iframe src if present
        if (rawLink.contains("<iframe", ignoreCase = true)) {
            val matcher = Pattern.compile("src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(rawLink)
            if (matcher.find()) {
                cleanLink = matcher.group(1).orEmpty()
            }
        }

        // If link points to web/html player (e.g. ayvspop player or ok.ru or soccerfull)
        if (cleanLink.contains("ayvspop.github.io", ignoreCase = true) && cleanLink.contains("?play=")) {
            // Extract the actual direct play URL
            val playParam = cleanLink.substringAfter("?play=")
            val (directUrl, headers) = parseStreamUrlAndHeaders(playParam)
            return StreamItem(
                channelName = channelTitle,
                fullUrl = cleanLink,
                cleanUrl = directUrl,
                isIframeOrWeb = false,
                headers = headers
            )
        }

        val isWeb = cleanLink.startsWith("http") && (cleanLink.contains("ok.ru") || cleanLink.contains("dailymotion") || cleanLink.contains("soccerfull.net") || cleanLink.contains("play/") || cleanLink.endsWith(".html") || !cleanLink.contains(".m3u8") && !cleanLink.contains(".mpd") && !cleanLink.contains(".mp4"))

        val (directUrl, headers) = parseStreamUrlAndHeaders(cleanLink)

        return StreamItem(
            channelName = channelTitle,
            fullUrl = cleanLink,
            cleanUrl = directUrl,
            isIframeOrWeb = isWeb,
            headers = headers
        )
    }

    private fun parseM3uChannels(
        m3uContent: String,
        isTapmad: Boolean,
        favoriteIds: Set<String>
    ): List<ChannelItem> {
        val list = mutableListOf<ChannelItem>()
        val lines = m3uContent.lines()
        var currentLogo = ""
        var currentGroup = if (isTapmad) "Tapmad Sports" else "General"
        var currentTitle = ""

        val extInfRegex = Regex("""#EXTINF:-1(?:[^\n]*tvg-logo="([^"]*)")?(?:[^\n]*group-title="([^"]*)")?,([^\n\r]+)""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF")) {
                val match = extInfRegex.find(trimmed)
                if (match != null) {
                    currentLogo = match.groupValues.getOrNull(1).orEmpty()
                    currentGroup = match.groupValues.getOrNull(2)?.ifBlank { if (isTapmad) "Tapmad Live" else "Live TV" } ?: if (isTapmad) "Tapmad Live" else "Live TV"
                    currentTitle = match.groupValues.getOrNull(3)?.trim().orEmpty()
                } else {
                    currentTitle = trimmed.substringAfter(",").trim()
                }
            } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                if (currentTitle.isNotBlank()) {
                    val id = generateMatchId(if (isTapmad) "tapmad" else "tv", currentGroup, currentTitle, "", "", trimmed)
                    list.add(
                        ChannelItem(
                            id = id,
                            title = currentTitle,
                            category = currentGroup,
                            logoUrl = currentLogo,
                            streamUrl = trimmed,
                            isTapmad = isTapmad,
                            isFavorite = favoriteIds.contains(id)
                        )
                    )
                }
                currentLogo = ""
                currentGroup = if (isTapmad) "Tapmad Sports" else "General"
                currentTitle = ""
            }
        }
        return list
    }

    private fun parseReplays(text: String, favoriteIds: Set<String>): List<HighlightItem> {
        val replays = mutableListOf<HighlightItem>()
        val blocks = text.split("# ")

        for (block in blocks) {
            if (block.isBlank()) continue
            val lines = block.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.isEmpty()) continue

            val title = lines[0]
            var category = "Sports"
            var tournament = "Match Replay"
            var thumb = ""
            var dateStr = ""
            val servers = mutableListOf<HighlightServer>()

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.startsWith("~")) {
                    val parts = line.removePrefix("~").trim().split("\t")
                    if (parts.isNotEmpty()) category = parts[0].trim()
                    if (parts.size > 1) tournament = parts[1].trim()
                    if (parts.size > 2) thumb = parts[2].trim()
                    if (parts.size > 3) dateStr = parts[3].trim()
                } else {
                    val parts = line.split("\t")
                    if (parts.size >= 3) {
                        servers.add(HighlightServer(serverName = parts[0].trim(), type = parts[1].trim(), streamUrl = parts[2].trim()))
                    } else if (parts.size == 2) {
                        servers.add(HighlightServer(serverName = parts[0].trim(), type = "iframe", streamUrl = parts[1].trim()))
                    }
                }
            }

            if (servers.isNotEmpty()) {
                val id = generateMatchId("replay", category, tournament, title, dateStr, "")
                replays.add(
                    HighlightItem(
                        id = id,
                        title = title,
                        category = category,
                        tournament = tournament,
                        thumbnailUrl = thumb,
                        dateString = dateStr,
                        servers = servers,
                        isFavorite = favoriteIds.contains(id)
                    )
                )
            }
        }
        return replays
    }

    suspend fun toggleFavoriteMatch(match: MatchItem) {
        if (match.isFavorite) {
            favoriteMatchDao.deleteFavorite(match.id)
        } else {
            val streamsJson = streamListAdapter.toJson(match.streams)
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

    suspend fun toggleFavoriteChannel(channel: ChannelItem) {
        if (channel.isFavorite) {
            favoriteMatchDao.deleteFavorite(channel.id)
        } else {
            val stream = StreamItem(
                channelName = channel.title,
                fullUrl = channel.streamUrl,
                cleanUrl = channel.streamUrl
            )
            val streamsJson = streamListAdapter.toJson(listOf(stream))
            val entity = FavoriteMatchEntity(
                matchId = channel.id,
                eventName = channel.title,
                tournamentName = channel.category,
                category = if (channel.isTapmad) "Tapmad" else "Live TV",
                teamA = channel.title,
                teamB = "",
                teamAFlag = channel.logoUrl,
                teamBFlag = "",
                tournamentLogo = channel.logoUrl,
                startTime = "",
                status = "LIVE",
                streamsJson = streamsJson
            )
            favoriteMatchDao.insertFavorite(entity)
        }
    }

    suspend fun toggleFavoriteHighlight(highlight: HighlightItem) {
        if (highlight.isFavorite) {
            favoriteMatchDao.deleteFavorite(highlight.id)
        } else {
            val streams = highlight.servers.map {
                StreamItem(
                    channelName = it.serverName,
                    fullUrl = it.streamUrl,
                    cleanUrl = it.streamUrl,
                    isIframeOrWeb = it.type.contains("iframe", ignoreCase = true)
                )
            }
            val streamsJson = streamListAdapter.toJson(streams)
            val entity = FavoriteMatchEntity(
                matchId = highlight.id,
                eventName = highlight.title,
                tournamentName = highlight.tournament,
                category = highlight.category,
                teamA = highlight.title,
                teamB = "",
                teamAFlag = highlight.thumbnailUrl,
                teamBFlag = "",
                tournamentLogo = highlight.thumbnailUrl,
                startTime = highlight.dateString,
                status = "HIGHLIGHT",
                streamsJson = streamsJson
            )
            favoriteMatchDao.insertFavorite(entity)
        }
    }

    private fun parseDateToEpochMs(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        val clean = dateStr.trim()

        val patterns = listOf(
            "yyyy/MM/dd HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd",
            "dd/MM/yyyy hh:mm:ss a",
            "MM/dd/yyyy hh:mm:ss a",
            "yyyy/MM/dd HH:mm:ss"
        )

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(clean)
                if (date != null) return date.time
            } catch (e: Exception) {
                // continue
            }
        }
        return 0L
    }

    private fun parseStatus(statusStr: String?): MatchStatus {
        return when (statusStr?.trim()?.uppercase()) {
            "LIVE" -> MatchStatus.LIVE
            "UPCOMING" -> MatchStatus.UPCOMING
            "ENDED" -> MatchStatus.ENDED
            else -> MatchStatus.UPCOMING
        }
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
