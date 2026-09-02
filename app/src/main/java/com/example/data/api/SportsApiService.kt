package com.example.data.api

import com.example.data.model.FirebaseEventDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class SportsApiService(
    private val client: OkHttpClient = createOkHttpClient()
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val eventListType = Types.newParameterizedType(List::class.java, FirebaseEventDto::class.java)
    private val eventListAdapter = moshi.adapter<List<FirebaseEventDto>>(eventListType)

    suspend fun getLiveSportsEvents(): List<FirebaseEventDto> = withContext(Dispatchers.IO) {
        val url = "https://livefy-tv-64d95-default-rtdb.firebaseio.com/sports_live/events.json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MukulSports/1.0 (Android; Mobile)")
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch sports events: ${response.code}")
            val body = response.body?.string() ?: return@use emptyList()
            eventListAdapter.fromJson(body) ?: emptyList()
        }
    }

    suspend fun getTapmadPlaylist(): String = withContext(Dispatchers.IO) {
        val url = "https://raw.githubusercontent.com/srhady/tapmad-bd/refs/heads/main/tapmad_bd.m3u?t=${System.currentTimeMillis()}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MukulSports/1.0 (Android; Mobile)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch Tapmad playlist: ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    suspend fun getBdipTvPlaylist(): String = withContext(Dispatchers.IO) {
        val url = "https://raw.githubusercontent.com/mesamirh/BDIP-Tv/refs/heads/main/playlist.m3u"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MukulSports/1.0 (Android; Mobile)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch BDIP TV playlist: ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    suspend fun getReplaysText(): String = withContext(Dispatchers.IO) {
        val url = "https://mukul-sports.ai.studio/api/replays/replays.txt"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MukulSports/1.0 (Android; Mobile)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch replays: ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    companion object {
        fun createOkHttpClient(): OkHttpClient {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()
        }
    }
}
