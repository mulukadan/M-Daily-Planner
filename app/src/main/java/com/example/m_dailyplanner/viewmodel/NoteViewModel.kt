package com.example.m_dailyplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow(0)
    val selectedCategoryId: StateFlow<Int> = _selectedCategoryId.asStateFlow()

    val categories: StateFlow<List<NoteCategory>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val noteCountsPerCategory: StateFlow<Map<Int, Int>> = repository.getNotesCountPerCategory()
        .map { list -> list.associate { it.categoryId to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val notes: StateFlow<List<Note>> = combine(_searchQuery, _selectedCategoryId) { q, catId ->
        q to catId
    }.flatMapLatest { (query, catId) ->
        if (catId == 0) emptyFlow()
        else if (query.isBlank()) repository.getNotesByCategory(catId)
        else repository.searchNotesByCategory(query, catId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            categories.collect { cats ->
                if (_selectedCategoryId.value == 0 && cats.isNotEmpty()) {
                    _selectedCategoryId.value = cats.first().id
                }
            }
        }
    }

    fun setSelectedCategory(categoryId: Int) {
        _selectedCategoryId.value = categoryId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun getNoteById(id: Int): Note? = repository.getNoteById(id)

    fun saveNote(note: Note, onSaved: ((Note) -> Unit)? = null) {
        viewModelScope.launch {
            if (note.id == 0) {
                val id = repository.insertNote(note)
                onSaved?.invoke(note.copy(id = id.toInt()))
            } else {
                val updated = note.copy(updatedAt = System.currentTimeMillis())
                repository.updateNote(updated)
                onSaved?.invoke(updated)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun addCategory(name: String, color: String) {
        viewModelScope.launch {
            repository.insertCategory(NoteCategory(name = name.trim(), color = color))
        }
    }

    fun updateCategory(category: NoteCategory) {
        viewModelScope.launch { repository.updateCategory(category) }
    }

    fun deleteCategory(category: NoteCategory, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.getNotesCountByCategory(category.id)
            if (count > 0) {
                onError("Cannot delete a category that still has notes")
            } else {
                repository.deleteCategory(category)
                if (_selectedCategoryId.value == category.id) {
                    val remaining = categories.value.filter { it.id != category.id }
                    _selectedCategoryId.value = remaining.firstOrNull()?.id ?: 0
                }
            }
        }
    }

    suspend fun getNotesCountByCategory(categoryId: Int): Int =
        repository.getNotesCountByCategory(categoryId)
}

class NoteViewModelFactory(
    private val application: Application,
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
