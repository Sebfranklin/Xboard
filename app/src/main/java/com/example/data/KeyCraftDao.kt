package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyCraftDao {

    // Learned Words
    @Query("SELECT * FROM learned_words ORDER BY isCustom DESC, frequency DESC, lastUsedAt DESC")
    fun observeLearnedWords(): Flow<List<LearnedWordEntity>>

    @Query("SELECT * FROM learned_words")
    suspend fun getAllLearnedWordsOnce(): List<LearnedWordEntity>

    @Query("SELECT * FROM learned_words WHERE word = :word LIMIT 1")
    suspend fun getLearnedWord(word: String): LearnedWordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearnedWord(entity: LearnedWordEntity)

    @Query("DELETE FROM learned_words WHERE word = :word")
    suspend fun deleteLearnedWord(word: String)

    @Query("DELETE FROM learned_words WHERE isCustom = 0")
    suspend fun clearAutoLearnedWords()

    // Learned Bigrams
    @Query("SELECT * FROM learned_bigrams ORDER BY count DESC, lastUsedAt DESC LIMIT 120")
    fun observeTopBigrams(): Flow<List<BigramEntity>>

    @Query("SELECT * FROM learned_bigrams")
    suspend fun getAllBigramsOnce(): List<BigramEntity>

    @Query("SELECT * FROM learned_bigrams WHERE prevWord = :prev AND nextWord = :next LIMIT 1")
    suspend fun getBigram(prev: String, next: String): BigramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBigram(entity: BigramEntity)

    @Query("DELETE FROM learned_bigrams")
    suspend fun clearAllBigrams()

    // Snippets
    @Query("SELECT * FROM snippets ORDER BY useCount DESC, id ASC")
    fun observeSnippets(): Flow<List<SnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: SnippetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippets(snippets: List<SnippetEntity>)

    @Update
    suspend fun updateSnippet(snippet: SnippetEntity)

    @Query("UPDATE snippets SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementSnippetUse(id: Int)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteSnippetById(id: Int)

    // Clipboard History
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, createdAt DESC LIMIT 25")
    fun observeClipboardItems(): Flow<List<ClipboardItemEntity>>

    @Query("SELECT * FROM clipboard_items WHERE content = :content LIMIT 1")
    suspend fun findClipboardByContent(content: String): ClipboardItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClipboardItem(item: ClipboardItemEntity)

    @Query("UPDATE clipboard_items SET isPinned = :pinned WHERE id = :id")
    suspend fun setClipboardPinned(id: Int, pinned: Boolean)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteClipboardItem(id: Int)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinnedClipboard()
}
