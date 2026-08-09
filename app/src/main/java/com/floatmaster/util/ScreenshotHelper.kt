package com.floatmaster.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * WHY: Screenshot + Save PDF — floating browser must let user save/share without leaving float. Uses PixelCopy for surface, PrintManager for PDF.
 */
object ScreenshotHelper {

    fun shareScreenshot(context: Context, webView: WebView) {
        try {
            // WHY: Create bitmap from WebView via Canvas — works even when not on screen (overlay)
            val bmp = Bitmap.createBitmap(webView.width.coerceAtLeast(1), webView.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            webView.draw(canvas)
            val file = File(context.cacheDir, "floatmaster_screenshot_\${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
            val uri = FileProvider.getUriForFile(context, "\${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            context.startActivity(Intent.createChooser(send, "Share screenshot").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) { android.widget.Toast.makeText(context, "Screenshot failed: \${e.message}", android.widget.Toast.LENGTH_SHORT).show() }
    }

    fun saveAsPdf(context: Context, webView: WebView, title: String = "FloatMaster") {
        try {
            // WHY: WebView.createPrintDocumentAdapter is the only reliable way to save HTML as PDF (keeps vector text)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val adapter = webView.createPrintDocumentAdapter(title)
            printManager.print(title, adapter, PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setColorMode(PrintAttributes.COLOR_MODE_COLOR).build())
        } catch (e: Exception) { android.widget.Toast.makeText(context, "PDF failed: \${e.message}", android.widget.Toast.LENGTH_SHORT).show() }
    }
}
