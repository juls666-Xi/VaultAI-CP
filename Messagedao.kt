package com.offlineai.data.repository
 
import androidx.room.*
import com.offlineai.data.model.Message
import kotlinx.coroutines.flow.Flow
 
/**
 * Data Access Object for [Message] entities.
 * All queries are exposed as Flow so the UI stays reactive.
 */
@Dao
interface MessageDao {
 
    /** Insert a new message; replace on conflict (shouldn't normally happen with autoGenerate). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: Message): Long
 
    /** Return all messages ordered from oldest to newest. */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>
 
    /** Delete every message — used for "Clear Chat" functionality. */
    @Query("DELETE FROM messages")
    suspend fun deleteAll()
 
    /** Return all messages as a plain list (used for export). */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<Message>
}
 