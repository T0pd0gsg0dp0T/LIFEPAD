package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepad.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId = :folderId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByFolder(folderId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folderId IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun getRootNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE title = :title LIMIT 1")
    suspend fun getNoteByTitle(title: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE LOWER(title) = LOWER(:title) LIMIT 1")
    suspend fun getNoteByTitleIgnoreCase(title: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT 10")
    suspend fun searchNotesByTitle(query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int

    // FTS search
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query
        ORDER BY notes.updatedAt DESC
    """)
    suspend fun searchNotes(query: String): List<NoteEntity>
}
