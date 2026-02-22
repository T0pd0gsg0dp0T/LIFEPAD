package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.BacklinkDao
import com.lifepad.app.data.local.dao.HashtagDao
import com.lifepad.app.data.local.dao.NoteDao
import com.lifepad.app.data.local.entity.ItemType
import com.lifepad.app.data.local.entity.NoteEntity
import com.lifepad.app.domain.parser.HashtagParser
import com.lifepad.app.domain.parser.WikilinkParser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val backlinkDao: BacklinkDao,
    private val hashtagDao: HashtagDao
) {
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>> = noteDao.getNotesByFolder(folderId)

    fun getRootNotes(): Flow<List<NoteEntity>> = noteDao.getRootNotes()

    fun getPinnedNotes(): Flow<List<NoteEntity>> = noteDao.getPinnedNotes()

    fun observeNote(noteId: Long): Flow<NoteEntity?> = noteDao.observeNoteById(noteId)

    suspend fun getNoteById(noteId: Long): NoteEntity? = noteDao.getNoteById(noteId)

    suspend fun getNoteByTitle(title: String): NoteEntity? = noteDao.getNoteByTitle(title)

    suspend fun getNoteByTitleIgnoreCase(title: String): NoteEntity? = noteDao.getNoteByTitleIgnoreCase(title)

    suspend fun searchNotesByTitle(query: String): List<NoteEntity> = noteDao.searchNotesByTitle(query)

    suspend fun searchNotes(query: String): List<NoteEntity> = noteDao.searchNotes("$query*")

    fun getBacklinksForNote(noteId: Long): Flow<List<NoteEntity>> = backlinkDao.getBacklinksForNote(noteId)

    fun getOutgoingLinksForNote(noteId: Long): Flow<List<NoteEntity>> = backlinkDao.getOutgoingLinksForNote(noteId)

    suspend fun getBacklinkCount(noteId: Long): Int = backlinkDao.getBacklinkCount(noteId)

    suspend fun saveNote(note: NoteEntity): Long {
        val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
        val noteId = noteDao.insert(updatedNote)
        val savedNoteId = if (note.id == 0L) noteId else note.id

        // Sync hashtags
        val hashtags = HashtagParser.extractHashtags(note.content)
        hashtagDao.syncHashtagsForItem(ItemType.NOTE, savedNoteId, hashtags)

        // Sync wikilinks/backlinks
        val wikilinkTitles = WikilinkParser.extractWikilinks(note.content)
        val targetNoteIds = wikilinkTitles.mapNotNull { title ->
            noteDao.getNoteByTitleIgnoreCase(title)?.id
        }
        backlinkDao.updateBacklinksForNote(savedNoteId, targetNoteIds)

        return savedNoteId
    }

    suspend fun deleteNote(noteId: Long) {
        // Hashtag usages and backlinks are cascade-deleted via foreign keys
        noteDao.deleteById(noteId)
    }

    suspend fun togglePin(noteId: Long) {
        val note = noteDao.getNoteById(noteId) ?: return
        noteDao.update(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun moveNote(noteId: Long, folderId: Long?) {
        noteDao.updateFolder(noteId, folderId, System.currentTimeMillis())
    }

    suspend fun getNoteCount(): Int = noteDao.getNoteCount()

    fun getHashtagsForNote(noteId: Long) = hashtagDao.observeHashtagsForItem(ItemType.NOTE, noteId)
}
