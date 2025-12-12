package com.porvida.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatMessage(
    @PrimaryKey val id: String,
    val userId: String,
    val conversationId: String = "default",
    val role: String, // "user" | "assistant"
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
