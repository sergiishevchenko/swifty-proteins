package com.music42.swiftyprotein.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.music42.swiftyprotein.data.local.entity.User

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User)

    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = :id")
    suspend fun updatePasswordHash(id: Long, passwordHash: String)
}
