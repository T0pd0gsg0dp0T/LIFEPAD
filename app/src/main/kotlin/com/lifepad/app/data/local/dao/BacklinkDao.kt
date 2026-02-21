package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lifepad.app.data.local.entity.BacklinkEntity
import com.lifepad.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BacklinkDao {
    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN backlinks ON notes.id = backlinks.sourceNoteId
        WHERE backlinks.targetNoteId = :noteId
    """)
    fun getBacklinksForNote(noteId: Long): Flow<List<NoteEntity>>

    @Query("""
        SELECT notes.* FROM notes
        INNER JOIN backlinks ON notes.id = backlinks.targetNoteId
        WHERE backlinks.sourceNoteId = :noteId
    """)
    fun getOutgoingLinksForNote(noteId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM backlinks WHERE sourceNoteId = :sourceNoteId")
    suspend fun getBacklinksBySource(sourceNoteId: Long): List<BacklinkEntity>

    @Query("SELECT COUNT(*) FROM backlinks WHERE targetNoteId = :noteId")
    suspend fun getBacklinkCount(noteId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(backlink: BacklinkEntity)

    @Query("DELETE FROM backlinks WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteBySource(sourceNoteId: Long)

    @Query("DELETE FROM backlinks WHERE sourceNoteId = :sourceId AND targetNoteId = :targetId")
    suspend fun delete(sourceId: Long, targetId: Long)

    @Transaction
    suspend fun updateBacklinksForNote(sourceNoteId: Long, targetNoteIds: List<Long>) {
        deleteBySource(sourceNoteId)
        targetNoteIds.forEach { targetId ->
            insert(BacklinkEntity(sourceNoteId = sourceNoteId, targetNoteId = targetId))
        }
    }
}
