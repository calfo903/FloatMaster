package com.floatmaster.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * WHY: Share browser output without broad storage permissions. Files are created only inside the FileProvider share cache.
 */
object ScreenshotHelper {
    fun shareScreenshot(context: Context, webView: WebView) {
        runCatching {
            val width = webView.width.coerceAtLeast(1)
            val height = webView.height.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).also(webView::draw)

            val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
            val file = File(shareDir, "floatmaster_screenshot_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 90, output) }
            bitmap.recycle()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "Share screenshot").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }.onFailure { error ->
            android.widget.Toast.makeText(context, "Screenshot failed: ${error.message ?: "unknown error"}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun saveAsPdf(context: Context, webView: WebView, title: String = "FloatMaster") {
        runCatching {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val adapter = webView.createPrintDocumentAdapter(title.take(80))
            printManager.print(
                title.take(80),
                adapter,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build()
            )
        }.onFailure { error ->
            android.widget.Toast.makeText(context, "PDF failed: ${error.message ?: "unknown error"}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
