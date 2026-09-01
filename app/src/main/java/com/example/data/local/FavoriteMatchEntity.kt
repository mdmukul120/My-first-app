package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_matches")
data class FavoriteMatchEntity(
    @PrimaryKey
    val matchId: String,
    val eventName: String,
    val tournamentName: String,
    val category: String,
    val teamA: String,
    val teamB: String,
    val teamAFlag: String,
    val teamBFlag: String,
    val tournamentLogo: String,
    val startTime: String,
    val status: String,
    val streamsJson: String,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)
