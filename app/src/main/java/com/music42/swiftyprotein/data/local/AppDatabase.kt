package com.music42.swiftyprotein.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.music42.swiftyprotein.data.local.entity.User

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
