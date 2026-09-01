package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMatchDao {
    @Query("SELECT * FROM favorite_matches ORDER BY savedAtTimestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteMatchEntity>>

    @Query("SELECT matchId FROM favorite_matches")
    fun getFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(match: FavoriteMatchEntity)

    @Query("DELETE FROM favorite_matches WHERE matchId = :matchId")
    suspend fun deleteFavorite(matchId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_matches WHERE matchId = :matchId)")
    fun isFavorite(matchId: String): Flow<Boolean>
}
