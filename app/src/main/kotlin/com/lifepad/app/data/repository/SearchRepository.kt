package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.SearchDao
import com.lifepad.app.data.local.dao.SearchResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchDao: SearchDao
) {
    /**
     * Search across all modules in parallel
     * @param query The search query
     * @param types Which item types to search (null = all)
     * @return Combined and sorted search results
     */
    suspend fun search(
        query: String,
        types: Set<String>? = null,
        limit: Int = 50
    ): List<SearchResult> = coroutineScope {
        val ftsQuery = "$query*"

        val searchNotes = types == null || "NOTE" in types
        val searchEntries = types == null || "ENTRY" in types
        val searchTransactions = types == null || "TRANSACTION" in types

        val notesDeferred = if (searchNotes) {
            async { searchDao.searchNotes(ftsQuery, limit) }
        } else null

        val entriesDeferred = if (searchEntries) {
            async { searchDao.searchJournalEntries(ftsQuery, limit) }
        } else null

        val transactionsDeferred = if (searchTransactions) {
            async { searchDao.searchTransactions(query, limit) }
        } else null

        val results = mutableListOf<SearchResult>()
        notesDeferred?.await()?.let { results.addAll(it) }
        entriesDeferred?.await()?.let { results.addAll(it) }
        transactionsDeferred?.await()?.let { results.addAll(it) }

        // Sort by updatedAt descending and limit
        results.sortedByDescending { it.updatedAt }.take(limit)
    }

    /**
     * Search by hashtag across all modules
     */
    suspend fun searchByHashtag(hashtagName: String, limit: Int = 50): List<SearchResult> {
        val normalized = hashtagName.lowercase().removePrefix("#")
        return searchDao.searchByHashtag(normalized, limit)
    }
}
