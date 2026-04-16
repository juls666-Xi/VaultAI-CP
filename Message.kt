package com.offlineai.data.model
 
import androidx.room.Entity
import androidx.room.PrimaryKey
 
/**
 * Represents a single message in the chat.
 *
 * @param id        Auto-generated primary key used by Room.
 * @param content   The text content of the message.
 * @param isUser    True = sent by the user; false = AI response.
 * @param timestamp Unix epoch milliseconds — used to sort messages.
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
 