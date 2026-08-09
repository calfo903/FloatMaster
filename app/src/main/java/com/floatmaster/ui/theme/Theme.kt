package com.floatmaster.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF6750A4),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFEADDFF),
    secondary = androidx.compose.ui.graphics.Color(0xFF625B71),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFE),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE7E0EC)
)
private val DarkScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF381E72),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF4F378B),
    secondary = androidx.compose.ui.graphics.Color(0xFFCCC2DC),
    surface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F)
)

@Composable
fun FloatMasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography(), content = content)
}
