package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query

data class SearchResult(
    val type: String,
    val id: Long,
    val title: String,
    val snippet: String,
    val updatedAt: Long
)

@Dao
interface SearchDao {
    // Search notes via FTS
    @Query("""
        SELECT 'NOTE' as type, n.id, n.title as title,
               SUBSTR(n.content, 1, 100) as snippet, n.updatedAt
        FROM notes n
        JOIN notes_fts ON n.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query
        ORDER BY n.updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchNotes(query: String, limit: Int = 50): List<SearchResult>

    // Search journal entries via FTS
    @Query("""
        SELECT 'ENTRY' as type, j.id, '' as title,
               SUBSTR(j.content, 1, 100) as snippet, j.updatedAt
        FROM journal_entries j
        JOIN journal_entries_fts ON j.rowid = journal_entries_fts.rowid
        WHERE journal_entries_fts MATCH :query
        ORDER BY j.updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchJournalEntries(query: String, limit: Int = 50): List<SearchResult>

    // Search transactions by description (no FTS, simpler)
    @Query("""
        SELECT 'TRANSACTION' as type, t.id, '' as title,
               t.description as snippet, t.updatedAt
        FROM transactions t
        WHERE t.description LIKE '%' || :query || '%'
        ORDER BY t.updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchTransactions(query: String, limit: Int = 50): List<SearchResult>

    // Search by hashtag across all modules
    @Query("""
        SELECT
            CASE hu.itemType
                WHEN 'NOTE' THEN 'NOTE'
                WHEN 'ENTRY' THEN 'ENTRY'
                WHEN 'TRANSACTION' THEN 'TRANSACTION'
            END as type,
            hu.itemId as id,
            COALESCE(n.title, '') as title,
            COALESCE(
                SUBSTR(n.content, 1, 100),
                SUBSTR(j.content, 1, 100),
                t.description
            ) as snippet,
            COALESCE(n.updatedAt, j.updatedAt, t.updatedAt) as updatedAt
        FROM hashtag_usage hu
        JOIN hashtags h ON hu.hashtagId = h.id
        LEFT JOIN notes n ON hu.itemType = 'NOTE' AND hu.itemId = n.id
        LEFT JOIN journal_entries j ON hu.itemType = 'ENTRY' AND hu.itemId = j.id
        LEFT JOIN transactions t ON hu.itemType = 'TRANSACTION' AND hu.itemId = t.id
        WHERE h.name = :hashtagName
        ORDER BY updatedAt DESC
        LIMIT :limit
    """)
    suspend fun searchByHashtag(hashtagName: String, limit: Int = 50): List<SearchResult>
}
