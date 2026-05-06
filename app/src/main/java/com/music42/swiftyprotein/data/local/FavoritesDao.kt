package com.music42.swiftyprotein.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.music42.swiftyprotein.data.local.entity.FavoriteLigand
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_ligands ORDER BY createdAtMs DESC")
    fun observeFavorites(): Flow<List<FavoriteLigand>>

    @Query("SELECT ligandId FROM favorite_ligands")
    fun observeFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteLigand)

    @Query("DELETE FROM favorite_ligands WHERE ligandId = :ligandId")
    suspend fun removeByLigandId(ligandId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_ligands WHERE ligandId = :ligandId)")
    suspend fun isFavorite(ligandId: String): Boolean
}
