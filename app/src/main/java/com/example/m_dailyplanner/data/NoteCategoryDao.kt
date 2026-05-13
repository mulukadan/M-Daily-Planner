package com.example.m_dailyplanner.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class CategoryNoteCount(val categoryId: Int, val count: Int)

@Dao
interface NoteCategoryDao {

    @Query("SELECT * FROM note_categories ORDER BY createdAt ASC")
    fun getAllCategories(): Flow<List<NoteCategory>>

    @Query("SELECT * FROM note_categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): NoteCategory?

    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :categoryId")
    suspend fun getNotesCountByCategory(categoryId: Int): Int

    @Query("SELECT categoryId, COUNT(*) as count FROM notes GROUP BY categoryId")
    fun getNotesCountPerCategory(): Flow<List<CategoryNoteCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: NoteCategory): Long

    @Update
    suspend fun updateCategory(category: NoteCategory)

    @Delete
    suspend fun deleteCategory(category: NoteCategory)
}
