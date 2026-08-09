package com.floatmaster.apps.document

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.floatmaster.model.FloatingWindow
import java.io.File

@Composable
fun FloatingDocumentViewerContent(window: FloatingWindow) {
    val context = LocalContext.current
    var filePath by remember { mutableStateOf<String?>(window.url) }
    var isPdf by remember { mutableStateOf(filePath?.endsWith(".pdf", true) == true) }
    var currentPage by remember { mutableStateOf(0) }
    var pageCount by remember { mutableStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // copy to cache and display
            val input = context.contentResolver.openInputStream(it)
            val outFile = File(context.cacheDir, "doc_${System.currentTimeMillis()}.pdf")
            input?.use { ins -> outFile.outputStream().use { outs -> ins.copyTo(outs) } }
            filePath = outFile.absolutePath
            isPdf = outFile.name.endsWith(".pdf", true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch("*/*") }) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Open") }
            if (filePath != null) {
                Text(File(filePath!!).name.take(24), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { filePath = null }) { Icon(Icons.Default.Close, null) }
            }
        }
        Divider()
        when {
            filePath == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PictureAsPdf, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("No document opened", style = MaterialTheme.typography.bodyMedium)
                    Text("Tap Open to select PDF / DOC / TXT", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            isPdf -> {
                // Use AndroidPdfViewer or fallback WebView Google Docs viewer
                // For brevity, use WebView with Google Docs viewer for remote, and PdfRenderer stream for local
                // Simple WebView approach works for both via file:// + viewer
                val viewerUrl = if (filePath!!.startsWith("http")) {
                    "https://docs.google.com/gview?embedded=true&url=$filePath"
                } else {
                    // For local PDFs, use WebView trick or show placeholder
                    // We'll use WebView loading file via content provider
                    null
                }
                if (viewerUrl != null) {
                    AndroidView(factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.allowFileAccess = true
                            webViewClient = WebViewClient()
                            loadUrl(viewerUrl)
                        }
                    }, modifier = Modifier.fillMaxSize())
                } else {
                    // Local PDF: show page count via PdfRenderer
                    LaunchedEffect(filePath) {
                        try {
                            val fd = ParcelFileDescriptor.open(File(filePath!!), ParcelFileDescriptor.MODE_READ_ONLY)
                            val renderer = PdfRenderer(fd)
                            pageCount = renderer.pageCount
                            renderer.close(); fd.close()
                        } catch (_: Exception) { pageCount = 0 }
                    }
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        Text("PDF: ${File(filePath!!).name}", style = MaterialTheme.typography.titleSmall)
                        Text("$pageCount pages", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        // Page navigation
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (currentPage > 0) currentPage-- }) { Icon(Icons.Default.NavigateBefore, null) }
                            Text("Page ${currentPage + 1} / $pageCount")
                            IconButton(onClick = { if (currentPage < pageCount - 1) currentPage++ }) { Icon(Icons.Default.NavigateNext, null) }
                        }
                        // In production, render bitmap via PdfRenderer.Page.render()
                        Box(Modifier.fillMaxSize().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                            Text("PDF rendering via PdfRenderer → Bitmap + Image()\n(Implement bitmap cache for smooth scroll)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
            else -> {
                // TXT / DOC fallback via WebView Google Docs viewer or plain text
                AndroidView(factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        // Try to load file; for .txt just load directly
                        loadUrl("file://$filePath")
                    }
                }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
