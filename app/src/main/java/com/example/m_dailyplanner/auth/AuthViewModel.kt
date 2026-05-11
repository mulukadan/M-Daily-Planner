package com.example.m_dailyplanner.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.m_dailyplanner.data.TaskDatabase
import com.example.m_dailyplanner.sync.FirestoreSync
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application,
    private val authManager: AuthManager,
    private val firestoreSync: FirestoreSync,
    private val database: TaskDatabase
) : AndroidViewModel(application) {

    companion object {
        // Replace with your Web Client ID from Firebase Console →
        // Authentication → Sign-in providers → Google → Web client ID
        const val WEB_CLIENT_ID = "699261682359-iht6jkerm5b3qulrud93fsdhrlcr3652.apps.googleusercontent.com"
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            authManager.signInWithGoogle(activityContext, WEB_CLIENT_ID)
                .onSuccess { user ->
                    _currentUser.value = user
                    mergeLocalAndCloudData()
                }
                .onFailure { e ->
                    _error.value = when {
                        e.message?.contains("canceled", ignoreCase = true) == true -> null
                        else -> "Sign-in failed. Please try again."
                    }
                }
            _isLoading.value = false
        }
    }

    fun signOut() {
        authManager.signOut()
        _currentUser.value = null
    }

    fun clearError() {
        _error.value = null
    }

    // Push local Room data up, then pull any cloud-only data down.
    // Uses REPLACE strategy so conflicts resolve to the most-recently-written record.
    private suspend fun mergeLocalAndCloudData() {
        val taskDao = database.taskDao()
        val projectDao = database.projectDao()
        val projectTaskDao = database.projectTaskDao()
        val noteDao = database.noteDao()

        // Push local → Firestore
        taskDao.getAllTasksList().forEach { firestoreSync.upsertTask(it) }
        projectDao.getAllProjectsList().forEach { firestoreSync.upsertProject(it) }
        projectTaskDao.getAllProjectTasksList().forEach { firestoreSync.upsertProjectTask(it) }
        noteDao.getAllNotesList().forEach { firestoreSync.upsertNote(it) }

        // Pull Firestore → Room (REPLACE handles deduplication by id)
        firestoreSync.fetchAllProjects().forEach { projectDao.insertProject(it) }
        firestoreSync.fetchAllProjectTasks().forEach { projectTaskDao.insertTask(it) }
        firestoreSync.fetchAllTasks().forEach { taskDao.insertTask(it) }
        firestoreSync.fetchAllNotes().forEach { noteDao.insertNote(it) }
    }
}

class AuthViewModelFactory(
    private val application: Application,
    private val authManager: AuthManager,
    private val firestoreSync: FirestoreSync,
    private val database: TaskDatabase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(application, authManager, firestoreSync, database) as T
    }
}
