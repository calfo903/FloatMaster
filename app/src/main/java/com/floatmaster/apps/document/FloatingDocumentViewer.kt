package com.floatmaster.apps.document

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.floatmaster.model.FloatingWindow
import java.io.File
import java.net.URLDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local document viewer.
 * WHY: User-selected documents are copied into app-private cache and rendered locally; no file:// WebView and no storage permission are required.
 */
@Composable
fun FloatingDocumentViewerContent(window: FloatingWindow) {
    val context = LocalContext.current
    var filePath by remember { mutableStateOf(window.url?.takeIf { File(it).isFile } ) }
    var mimeType by remember { mutableStateOf("application/pdf") }
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val type = context.contentResolver.getType(uri).orEmpty()
            val extension = when {
                type == "application/pdf" -> "pdf"
                type.startsWith("text/") -> "txt"
                else -> "bin"
            }
            val dir = File(context.cacheDir, "documents").apply { mkdirs() }
            val outFile = File(dir, "doc_${System.currentTimeMillis()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input -> outFile.outputStream().use(input::copyTo) }
                ?: error("Unable to read document")
            filePath = outFile.absolutePath
            mimeType = type.ifBlank { if (extension == "pdf") "application/pdf" else "text/plain" }
            currentPage = 0
            error = null
        }.onFailure { error = it.message ?: "Unable to open document" }
    }

    LaunchedEffect(filePath, mimeType, currentPage) {
        bitmap?.recycle()
        bitmap = null
        pageCount = 0
        val path = filePath ?: return@LaunchedEffect
        if (mimeType != "application/pdf") return@LaunchedEffect
        val rendered = withContext(Dispatchers.IO) {
            runCatching {
                ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        pageCount = renderer.pageCount
                        if (currentPage !in 0 until renderer.pageCount) return@use null
                        renderer.openPage(currentPage).use { page ->
                            val scale = 1.5f
                            val width = (page.width * scale).toInt().coerceAtMost(2400)
                            val height = (page.height * scale).toInt().coerceAtMost(3200)
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
                                output.eraseColor(android.graphics.Color.WHITE)
                                page.render(output, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }.getOrElse {
                error = it.message ?: "PDF rendering failed"
                null
            }
        }
        bitmap = rendered
    }

    DisposableEffect(Unit) {
        onDispose { bitmap?.recycle() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch("*/*") }) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.width(6.dp))
                Text("Open")
            }
            filePath?.let { path ->
                Text(File(path).name.take(24), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                IconButton(onClick = { filePath = null; bitmap = null }) { Icon(Icons.Default.Close, "Close") }
            }
        }
        HorizontalDivider()

        when {
            filePath == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("No document opened")
                }
            }
            mimeType == "application/pdf" -> Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = currentPage > 0, onClick = { currentPage-- }) { Icon(Icons.Default.NavigateBefore, "Previous page") }
                    Text("Page ${if (pageCount == 0) 0 else currentPage + 1} / $pageCount")
                    IconButton(enabled = currentPage + 1 < pageCount, onClick = { currentPage++ }) { Icon(Icons.Default.NavigateNext, "Next page") }
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    bitmap?.let { Image(it.asImageBitmap(), contentDescription = "PDF page ${currentPage + 1}", modifier = Modifier.fillMaxSize().padding(8.dp)) }
                        ?: CircularProgressIndicator()
                }
            }
            mimeType.startsWith("text/") -> {
                val text = remember(filePath) { filePath?.let { runCatching { File(it).readText().take(200_000) }.getOrNull() } }
                Box(Modifier.fillMaxSize().padding(12.dp)) { Text(text ?: error ?: "Unable to read text document") }
            }
            else -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(error ?: "This document type is not supported locally.")
            }
        }
    }
}
