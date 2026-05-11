package com.example.m_dailyplanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.m_dailyplanner.data.Note
import com.example.m_dailyplanner.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isNew = noteId == 0

    LaunchedEffect(noteId) {
        if (!isNew) {
            val loaded = viewModel.getNoteById(noteId)
            note = loaded
            title = loaded?.title ?: ""
            content = loaded?.content ?: ""
        }
    }

    val contentFocus = remember { FocusRequester() }

    fun save() {
        if (title.isBlank() && content.isBlank()) return
        val toSave = if (!isNew && note != null) {
            note!!.copy(title = title.trim(), content = content.trim())
        } else {
            Note(title = title.trim(), content = content.trim())
        }
        viewModel.saveNote(toSave)
    }

    BackHandler {
        save()
        onBack()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note") },
            text = { Text("Delete this note? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        note?.let { viewModel.deleteNote(it) }
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "New Note" else "Note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { save(); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew && note != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("Title", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                textStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text("Start writing…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp)
                    .padding(horizontal = 4.dp)
                    .focusRequester(contentFocus),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}
