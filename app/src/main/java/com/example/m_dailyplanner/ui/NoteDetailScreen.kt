package com.example.m_dailyplanner.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.m_dailyplanner.data.*
import com.example.m_dailyplanner.viewmodel.NoteViewModel
import org.json.JSONArray
import org.json.JSONObject

// ── Serialization ────────────────────────────────────────────────────────────

private fun colorToHex(c: Color): String = "#%08X".format(c.toArgb())

private fun hexToColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color.Unspecified
}

private fun serializeContent(a: AnnotatedString): String {
    if (a.spanStyles.isEmpty()) return a.text
    val root = JSONObject()
    root.put("v", 1); root.put("t", a.text)
    val arr = JSONArray()
    a.spanStyles.forEach { r ->
        val o = JSONObject()
        o.put("a", r.start); o.put("b", r.end)
        r.item.fontWeight?.let { o.put("fw", it.weight) }
        if (r.item.fontStyle == FontStyle.Italic) o.put("fs", "i")
        if (r.item.textDecoration == TextDecoration.LineThrough) o.put("td", "lt")
        if (r.item.color != Color.Unspecified) o.put("fc", colorToHex(r.item.color))
        if (r.item.background != Color.Unspecified) o.put("bg", colorToHex(r.item.background))
        arr.put(o)
    }
    root.put("s", arr)
    return root.toString()
}

private fun deserializeContent(raw: String): AnnotatedString {
    if (!raw.startsWith("{")) return AnnotatedString(raw)
    return try {
        val root = JSONObject(raw)
        val text = root.getString("t")
        val builder = AnnotatedString.Builder(text)
        val arr = root.optJSONArray("s") ?: return AnnotatedString(text)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val start = o.getInt("a"); val end = o.getInt("b")
            if (start >= end || start < 0 || end > text.length) continue
            builder.addStyle(SpanStyle(
                fontWeight = o.optInt("fw", 0).takeIf { it > 0 }?.let { FontWeight(it) },
                fontStyle = if (o.optString("fs") == "i") FontStyle.Italic else null,
                textDecoration = if (o.optString("td") == "lt") TextDecoration.LineThrough else null,
                color = o.optString("fc").takeIf { it.isNotEmpty() }?.let { hexToColor(it) } ?: Color.Unspecified,
                background = o.optString("bg").takeIf { it.isNotEmpty() }?.let { hexToColor(it) } ?: Color.Unspecified
            ), start, end)
        }
        builder.toAnnotatedString()
    } catch (_: Exception) {
        AnnotatedString(raw)
    }
}

// ── Span manipulation ─────────────────────────────────────────────────────────

private fun toggleSpan(
    value: TextFieldValue,
    style: SpanStyle,
    matches: (SpanStyle) -> Boolean
): TextFieldValue {
    if (value.selection.collapsed) return value
    val start = value.selection.min; val end = value.selection.max
    val a = value.annotatedString
    val hasIt = a.spanStyles.any { r -> matches(r.item) && r.start <= start && r.end >= end }
    val builder = AnnotatedString.Builder(a.text)
    a.spanStyles.forEach { r ->
        if (matches(r.item) && r.start < end && r.end > start) {
            if (r.start < start) builder.addStyle(r.item, r.start, start)
            if (r.end > end) builder.addStyle(r.item, end, r.end)
        } else {
            builder.addStyle(r.item, r.start, r.end)
        }
    }
    if (!hasIt) builder.addStyle(style, start, end)
    return value.copy(annotatedString = builder.toAnnotatedString())
}

private fun applyColorSpan(value: TextFieldValue, color: Color, isHighlight: Boolean): TextFieldValue {
    if (value.selection.collapsed) return value
    val start = value.selection.min; val end = value.selection.max
    val a = value.annotatedString
    val matchesProp: (SpanStyle) -> Boolean =
        if (isHighlight) ({ it.background != Color.Unspecified }) else ({ it.color != Color.Unspecified })
    val builder = AnnotatedString.Builder(a.text)
    a.spanStyles.forEach { r ->
        if (matchesProp(r.item) && r.start < end && r.end > start) {
            if (r.start < start) builder.addStyle(r.item, r.start, start)
            if (r.end > end) builder.addStyle(r.item, end, r.end)
        } else {
            builder.addStyle(r.item, r.start, r.end)
        }
    }
    builder.addStyle(
        if (isHighlight) SpanStyle(background = color) else SpanStyle(color = color),
        start, end
    )
    return value.copy(annotatedString = builder.toAnnotatedString())
}

private fun applyLinePrefix(value: TextFieldValue, prefix: String): TextFieldValue {
    val text = value.annotatedString.text
    val cursor = value.selection.min
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val lineEndRaw = text.indexOf('\n', cursor)
    val lineEnd = if (lineEndRaw == -1) text.length else lineEndRaw
    val line = text.substring(lineStart, lineEnd)

    return if (line.startsWith(prefix)) {
        val len = prefix.length
        val newText = text.removeRange(lineStart, lineStart + len)
        val builder = AnnotatedString.Builder(newText)
        value.annotatedString.spanStyles.forEach { r ->
            val ns = if (r.start >= lineStart + len) r.start - len else if (r.start >= lineStart) lineStart else r.start
            val ne = if (r.end >= lineStart + len) r.end - len else if (r.end >= lineStart) lineStart else r.end
            if (ns < ne) builder.addStyle(r.item, ns, ne)
        }
        val nc = (cursor - len).coerceAtLeast(lineStart)
        value.copy(annotatedString = builder.toAnnotatedString(), selection = TextRange(nc))
    } else {
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val shift = prefix.length
        val builder = AnnotatedString.Builder(newText)
        value.annotatedString.spanStyles.forEach { r ->
            val ns = if (r.start >= lineStart) r.start + shift else r.start
            val ne = if (r.end >= lineStart) r.end + shift else r.end
            builder.addStyle(r.item, ns, ne)
        }
        value.copy(annotatedString = builder.toAnnotatedString(), selection = TextRange(cursor + shift))
    }
}

// Preserve spans from previous value when IME changes text
private fun mergeSpans(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    if (old.text == new.text) return new.copy(annotatedString = old.annotatedString)
    val newText = new.text
    val builder = AnnotatedString.Builder(newText)
    old.annotatedString.spanStyles.forEach { r ->
        val s = r.start.coerceAtMost(newText.length)
        val e = r.end.coerceAtMost(newText.length)
        if (s < e) builder.addStyle(r.item, s, e)
    }
    return new.copy(annotatedString = builder.toAnnotatedString())
}

// ── Color presets ─────────────────────────────────────────────────────────────

private val FONT_COLORS = listOf(
    Color(0xFF000000), Color(0xFFD32F2F), Color(0xFFE65100), Color(0xFFF9A825),
    Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFFAD1457),
    Color(0xFF37474F), Color(0xFF78909C)
)

private val HIGHLIGHT_COLORS = listOf(
    Color(0xFFFFFF00), Color(0xFF90EE90), Color(0xFFADD8E6), Color(0xFFFFB6C1),
    Color(0xFFFFD700), Color(0xFFDDA0DD), Color(0xFF98FB98), Color(0xFFE6E6FA)
)

// ── Color picker dialog ───────────────────────────────────────────────────────

@Composable
private fun ColorPickerDialog(
    title: String,
    colors: List<Color>,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleSmall) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(colors) { c ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                            .clickable { onPick(c); onDismiss() }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Formatting toolbar ────────────────────────────────────────────────────────

@Composable
private fun FormattingToolbar(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit) {
    var showFontColor by remember { mutableStateOf(false) }
    var showHighlight by remember { mutableStateOf(false) }

    if (showFontColor) {
        ColorPickerDialog("Font Color", FONT_COLORS,
            onPick = { onValueChange(applyColorSpan(value, it, false)) },
            onDismiss = { showFontColor = false })
    }
    if (showHighlight) {
        ColorPickerDialog("Highlight", HIGHLIGHT_COLORS,
            onPick = { onValueChange(applyColorSpan(value, it, true)) },
            onDismiss = { showHighlight = false })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolBtn(Icons.Default.FormatBold, "Bold") {
            onValueChange(toggleSpan(value, SpanStyle(fontWeight = FontWeight.Bold)) { it.fontWeight == FontWeight.Bold })
        }
        ToolBtn(Icons.Default.FormatItalic, "Italic") {
            onValueChange(toggleSpan(value, SpanStyle(fontStyle = FontStyle.Italic)) { it.fontStyle == FontStyle.Italic })
        }
        ToolBtn(Icons.Default.FormatStrikethrough, "Strikethrough") {
            onValueChange(toggleSpan(value, SpanStyle(textDecoration = TextDecoration.LineThrough)) { it.textDecoration == TextDecoration.LineThrough })
        }
        VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))
        ToolBtn(Icons.Default.FormatColorText, "Font Color") { showFontColor = true }
        ToolBtn(Icons.Default.FormatColorFill, "Highlight") { showHighlight = true }
        VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))
        ToolBtn(Icons.Default.Title, "Heading") { onValueChange(applyLinePrefix(value, "# ")) }
        ToolBtn(Icons.Default.FormatListBulleted, "Bullet") { onValueChange(applyLinePrefix(value, "- ")) }
        ToolBtn(Icons.Default.CheckBoxOutlineBlank, "Checkbox") { onValueChange(applyLinePrefix(value, "- [ ] ")) }
    }
}

@Composable
private fun ToolBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(icon, contentDescription = desc, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private fun parseColorDetail(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    Color(0xFF6750A4.toInt())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    defaultCategoryId: Int = DEFAULT_CATEGORY_ID,
    viewModel: NoteViewModel,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf(TextFieldValue(AnnotatedString(""))) }
    var selectedCategoryId by remember { mutableStateOf(defaultCategoryId) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val isNew = noteId == 0

    val categories by viewModel.categories.collectAsState()

    LaunchedEffect(noteId) {
        if (!isNew) {
            val loaded = viewModel.getNoteById(noteId)
            note = loaded
            title = loaded?.title ?: ""
            content = TextFieldValue(deserializeContent(loaded?.content ?: ""))
            selectedCategoryId = loaded?.categoryId ?: defaultCategoryId
        }
    }

    LaunchedEffect(categories, defaultCategoryId) {
        if (selectedCategoryId == 0 && categories.isNotEmpty()) {
            selectedCategoryId = categories.first().id
        }
    }

    val contentFocus = remember { FocusRequester() }

    fun save() {
        if (title.isBlank() && content.text.isBlank()) return
        val toSave = if (note != null) {
            note!!.copy(title = title.trim(), content = serializeContent(content.annotatedString), categoryId = selectedCategoryId)
        } else {
            Note(title = title.trim(), content = serializeContent(content.annotatedString), categoryId = selectedCategoryId)
        }
        viewModel.saveNote(toSave) { saved -> note = saved }
    }

    BackHandler { save(); onBack() }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note") },
            text = { Text("Delete this note? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { note?.let { viewModel.deleteNote(it) }; showDeleteConfirm = false; onBack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
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
                    val hasContent = title.isNotBlank() || content.text.isNotBlank()
                    IconButton(onClick = { save() }, enabled = hasContent) {
                        Icon(Icons.Default.Save, "Save",
                            tint = if (hasContent) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                    }
                    if (!isNew && note != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
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
        ) {
            // Title
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("Title",
                        style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
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

            // Category selector
            if (categories.isNotEmpty()) {
                val selectedCategory = categories.find { it.id == selectedCategoryId }
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            leadingIcon = {
                                if (selectedCategory != null) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape)
                                        .background(parseColorDetail(selectedCategory.color)))
                                }
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(8.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(modifier = Modifier.size(12.dp).clip(CircleShape)
                                                .background(parseColorDetail(cat.color)))
                                            Text(cat.name)
                                        }
                                    },
                                    onClick = { selectedCategoryId = cat.id; categoryDropdownExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Formatting toolbar — pinned above content, always visible above keyboard
            FormattingToolbar(value = content, onValueChange = { content = it })

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Content — takes remaining space, scrollable within that space
            TextField(
                value = content,
                onValueChange = { content = mergeSpans(content, it) },
                placeholder = {
                    Text("Start writing…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
