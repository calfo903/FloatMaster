package com.floatmaster.apps.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatmaster.data.NotesRepository
import com.floatmaster.model.FloatingWindow
import kotlinx.coroutines.launch

@Composable
fun FloatingNotesContent(window: FloatingWindow) {
    val context = LocalContext.current
    val repo = remember { NotesRepository(context) }
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf(repo.getNotesForWindow(window.id)) }
    var selected by remember { mutableStateOf(notes.firstOrNull()) }
    var title by remember { mutableStateOf(selected?.title ?: "Untitled") }
    var body by remember { mutableStateOf(selected?.body ?: "") }
    var isBold by remember { mutableStateOf(false) }

    LaunchedEffect(selected?.id) {
        title = selected?.title ?: "Untitled"
        body = selected?.body ?: ""
    }

    Column(Modifier.fillMaxSize().padding(8.dp)) {
        // Page tabs
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(notes) { n ->
                    FilterChip(
                        selected = n.id == selected?.id,
                        onClick = { selected = n },
                        label = { Text(n.title.take(12)) }
                    )
                }
            }
            IconButton(onClick = {
                val n = repo.createNote(window.id, "Note ${notes.size + 1}")
                notes = repo.getNotesForWindow(window.id)
                selected = n
            }) { Icon(Icons.Default.Add, "New") }
        }
        OutlinedTextField(value = title, onValueChange = {
            title = it
            selected?.let { s ->
                repo.updateNote(s.copy(title = it))
                notes = repo.getNotesForWindow(window.id)
            }
        }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Title") })
        // Formatting bar
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { isBold = !isBold }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.FormatBold, "Bold", tint = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { body += "\n- " }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.FormatListBulleted, null) }
            IconButton(onClick = { body += "\n> " }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.FormatQuote, null) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                selected?.let { s ->
                    repo.updateNote(s.copy(title = title, body = body))
                    notes = repo.getNotesForWindow(window.id)
                }
            }) { Text("Save") }
            IconButton(onClick = {
                selected?.let { s -> repo.deleteNote(s.id); notes = repo.getNotesForWindow(window.id); selected = notes.firstOrNull() }
            }) { Icon(Icons.Default.Delete, "Delete") }
        }
        OutlinedTextField(
            value = body,
            onValueChange = {
                body = it
                selected?.let { s -> scope.launch { repo.updateNote(s.copy(body = it, title = title)) } }
            },
            modifier = Modifier.fillMaxSize(),
            placeholder = { Text("Start typing…") },
            label = { Text("Content") }
        )
    }
}
