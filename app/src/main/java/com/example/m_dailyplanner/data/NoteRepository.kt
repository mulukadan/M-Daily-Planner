package com.example.m_dailyplanner.data

import com.example.m_dailyplanner.sync.FirestoreSync
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val noteCategoryDao: NoteCategoryDao,
    private val firestoreSync: FirestoreSync
) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesByCategory(categoryId: Int): Flow<List<Note>> = noteDao.getNotesByCategory(categoryId)

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    fun searchNotesByCategory(query: String, categoryId: Int): Flow<List<Note>> =
        noteDao.searchNotesByCategory(query, categoryId)

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

    // Categories
    fun getAllCategories(): Flow<List<NoteCategory>> = noteCategoryDao.getAllCategories()

    fun getNotesCountPerCategory(): Flow<List<CategoryNoteCount>> =
        noteCategoryDao.getNotesCountPerCategory()

    suspend fun getCategoryById(id: Int): NoteCategory? = noteCategoryDao.getCategoryById(id)

    suspend fun getNotesCountByCategory(categoryId: Int): Int =
        noteCategoryDao.getNotesCountByCategory(categoryId)

    suspend fun insertCategory(category: NoteCategory): Long {
        val id = noteCategoryDao.insertCategory(category)
        firestoreSync.upsertNoteCategory(category.copy(id = id.toInt()))
        return id
    }

    suspend fun updateCategory(category: NoteCategory) {
        noteCategoryDao.updateCategory(category)
        firestoreSync.upsertNoteCategory(category)
    }

    suspend fun deleteCategory(category: NoteCategory) {
        noteCategoryDao.deleteCategory(category)
        firestoreSync.deleteNoteCategory(category.id)
    }
}
