package com.porvida.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AssistantMessage(
    @PrimaryKey val id: String,
    val userId: String,
    val role: String, // "user" | "assistant"
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
