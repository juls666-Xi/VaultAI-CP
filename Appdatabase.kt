package com.offlineai.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.offlineai.data.model.Message

/**
 * Room database definition.
 *
 * Room generates the concrete implementation at compile time.
 * We use a singleton (companion object) to prevent multiple DB instances.
 */
@Database(entities = [Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // Double-checked locking to prevent race conditions on first access
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offline_ai_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}