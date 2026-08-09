package com.floatmaster.apps.browser

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap

/** WHY: Keep browser bitmap conversion available without coupling the large browser surface to an extra import. */
fun Bitmap.asImageBitmap(): ImageBitmap = androidx.compose.ui.graphics.asImageBitmap(this)
