package com.example.m_dailyplanner.data

import com.example.m_dailyplanner.sync.FirestoreSync
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val firestoreSync: FirestoreSync
) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long {
        val id = noteDao.insertNote(note)
        firestoreSync.upsertNote(note.copy(id = id.toInt()))
        return id
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
        firestoreSync.upsertNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
        firestoreSync.deleteNote(note.id)
    }
}
