package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learned_words")
data class LearnedWordEntity(
    @PrimaryKey val word: String,
    val frequency: Int = 1,
    val isCustom: Boolean = true,
    val lastUsedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "learned_bigrams",
    primaryKeys = ["prevWord", "nextWord"]
)
data class BigramEntity(
    val prevWord: String,
    val nextWord: String,
    val count: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "snippets")
data class SnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trigger: String,
    val expansion: String,
    val category: String = "General",
    val isEnabled: Boolean = true,
    val useCount: Int = 0
)

@Entity(tableName = "clipboard_items")
data class ClipboardItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
