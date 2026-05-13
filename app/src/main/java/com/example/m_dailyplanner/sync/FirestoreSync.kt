package com.example.m_dailyplanner.sync

import com.example.m_dailyplanner.data.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirestoreSync {
    private val auth get() = Firebase.auth
    private val db get() = Firebase.firestore

    private val uid get() = auth.currentUser?.uid
    val isAvailable get() = uid != null

    private fun col(path: String) =
        db.collection("users").document(uid!!).collection(path)

    // ── Tasks ────────────────────────────────────────────────────────────────

    suspend fun upsertTask(task: Task) {
        if (!isAvailable) return
        col("tasks").document(task.id.toString()).set(task.toMap()).await()
    }

    suspend fun deleteTask(taskId: Int) {
        if (!isAvailable) return
        col("tasks").document(taskId.toString()).delete().await()
    }

    suspend fun fetchAllTasks(): List<Task> {
        if (!isAvailable) return emptyList()
        return col("tasks").get().await().documents.mapNotNull { it.data?.toTask() }
    }

    // ── Projects ─────────────────────────────────────────────────────────────

    suspend fun upsertProject(project: Project) {
        if (!isAvailable) return
        col("projects").document(project.id.toString()).set(project.toMap()).await()
    }

    suspend fun deleteProject(projectId: Int) {
        if (!isAvailable) return
        col("projects").document(projectId.toString()).delete().await()
    }

    suspend fun fetchAllProjects(): List<Project> {
        if (!isAvailable) return emptyList()
        return col("projects").get().await().documents.mapNotNull { it.data?.toProject() }
    }

    // ── Project Tasks ─────────────────────────────────────────────────────────

    suspend fun upsertProjectTask(task: ProjectTask) {
        if (!isAvailable) return
        col("project_tasks").document(task.id.toString()).set(task.toMap()).await()
    }

    suspend fun deleteProjectTask(taskId: Int) {
        if (!isAvailable) return
        col("project_tasks").document(taskId.toString()).delete().await()
    }

    suspend fun fetchAllProjectTasks(): List<ProjectTask> {
        if (!isAvailable) return emptyList()
        return col("project_tasks").get().await().documents.mapNotNull { it.data?.toProjectTask() }
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    suspend fun upsertNote(note: Note) {
        if (!isAvailable) return
        col("notes").document(note.id.toString()).set(note.toMap()).await()
    }

    suspend fun deleteNote(noteId: Int) {
        if (!isAvailable) return
        col("notes").document(noteId.toString()).delete().await()
    }

    suspend fun fetchAllNotes(): List<Note> {
        if (!isAvailable) return emptyList()
        return col("notes").get().await().documents.mapNotNull { it.data?.toNote() }
    }

    // ── Note Categories ───────────────────────────────────────────────────────

    suspend fun upsertNoteCategory(category: NoteCategory) {
        if (!isAvailable) return
        col("note_categories").document(category.id.toString()).set(category.toMap()).await()
    }

    suspend fun deleteNoteCategory(categoryId: Int) {
        if (!isAvailable) return
        col("note_categories").document(categoryId.toString()).delete().await()
    }

    suspend fun fetchAllNoteCategories(): List<NoteCategory> {
        if (!isAvailable) return emptyList()
        return col("note_categories").get().await().documents.mapNotNull { it.data?.toNoteCategory() }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    private fun Task.toMap() = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "date" to date,
        "time" to time,
        "reminderEnabled" to reminderEnabled,
        "status" to status,
        "priority" to priority,
        "position" to position,
        "createdAt" to createdAt
    )

    private fun Map<String, Any?>.toTask() = Task(
        id = (get("id") as? Long)?.toInt() ?: 0,
        name = get("name") as? String ?: "",
        description = get("description") as? String ?: "",
        date = get("date") as? String ?: "",
        time = get("time") as? String ?: "",
        reminderEnabled = get("reminderEnabled") as? Boolean ?: false,
        status = get("status") as? String ?: TaskStatus.PENDING.name,
        priority = get("priority") as? String ?: "Medium",
        position = (get("position") as? Long)?.toInt() ?: 0,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis()
    )

    private fun Project.toMap() = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "createdAt" to createdAt
    )

    private fun Map<String, Any?>.toProject() = Project(
        id = (get("id") as? Long)?.toInt() ?: 0,
        name = get("name") as? String ?: "",
        description = get("description") as? String ?: "",
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis()
    )

    private fun ProjectTask.toMap() = mapOf(
        "id" to id,
        "projectId" to projectId,
        "name" to name,
        "description" to description,
        "priority" to priority,
        "status" to status,
        "position" to position,
        "createdAt" to createdAt
    )

    private fun Map<String, Any?>.toProjectTask() = ProjectTask(
        id = (get("id") as? Long)?.toInt() ?: 0,
        projectId = (get("projectId") as? Long)?.toInt() ?: 0,
        name = get("name") as? String ?: "",
        description = get("description") as? String ?: "",
        priority = get("priority") as? String ?: "Medium",
        status = get("status") as? String ?: TaskStatus.PENDING.name,
        position = (get("position") as? Long)?.toInt() ?: 0,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis()
    )

    private fun Note.toMap() = mapOf(
        "id" to id,
        "title" to title,
        "content" to content,
        "categoryId" to categoryId,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun Map<String, Any?>.toNote() = Note(
        id = (get("id") as? Long)?.toInt() ?: 0,
        title = get("title") as? String ?: "",
        content = get("content") as? String ?: "",
        categoryId = (get("categoryId") as? Long)?.toInt() ?: DEFAULT_CATEGORY_ID,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis(),
        updatedAt = get("updatedAt") as? Long ?: System.currentTimeMillis()
    )

    private fun NoteCategory.toMap() = mapOf(
        "id" to id,
        "name" to name,
        "color" to color,
        "createdAt" to createdAt
    )

    private fun Map<String, Any?>.toNoteCategory() = NoteCategory(
        id = (get("id") as? Long)?.toInt() ?: 0,
        name = get("name") as? String ?: "",
        color = get("color") as? String ?: DEFAULT_CATEGORY_COLOR,
        createdAt = get("createdAt") as? Long ?: System.currentTimeMillis()
    )
}
