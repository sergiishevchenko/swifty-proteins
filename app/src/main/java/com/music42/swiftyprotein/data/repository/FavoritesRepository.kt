package com.music42.swiftyprotein.data.repository

import com.music42.swiftyprotein.data.local.FavoritesDao
import com.music42.swiftyprotein.data.local.entity.FavoriteLigand
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FavoritesRepository @Inject constructor(
    private val favoritesDao: FavoritesDao
) {
    fun observeFavorites(): Flow<List<FavoriteLigand>> = favoritesDao.observeFavorites()
    fun observeFavoriteIds(): Flow<List<String>> = favoritesDao.observeFavoriteIds()

    suspend fun toggleFavorite(ligandId: String): FavoriteToggleAction {
        return if (favoritesDao.isFavorite(ligandId)) {
            favoritesDao.removeByLigandId(ligandId)
            FavoriteToggleAction.Removed
        } else {
            favoritesDao.add(FavoriteLigand(ligandId = ligandId))
            FavoriteToggleAction.Added
        }
    }
}
