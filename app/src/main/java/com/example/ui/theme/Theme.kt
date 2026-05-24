package com.example.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Exclusive Custom Dark Theme
private val CustomDarkColorScheme = darkColorScheme(
    primary = BentoPrimaryDark,
    secondary = BentoPrimaryDark,
    tertiary = BentoRose,
    background = BentoBgDark,
    surface = BentoSurfaceDark,
    surfaceVariant = BentoSurfaceVariantDark,
    onPrimary = BentoSurfaceDark,
    onSecondary = BentoSurfaceDark,
    onBackground = BentoTextLight,
    onSurface = BentoTextLight,
    onSurfaceVariant = BentoOutline,
    error = BentoErrorDark,
    outline = BentoOutlineDark
)

// Light fallback theme
private val CustomLightColorScheme = lightColorScheme(
    primary = BentoPrimary,
    secondary = BentoPrimary,
    tertiary = BentoRose,
    background = BentoBgLight,
    surface = BentoSurfaceLight,
    surfaceVariant = Color(0xFFF3EDF7),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = BentoTextDark,
    onSurface = BentoTextDark,
    onSurfaceVariant = Color(0xFF49454F),
    error = BentoErrorLight,
    outline = BentoOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Light theme by default for the beautiful pastel Bento Grid
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CustomDarkColorScheme else CustomLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            var currentContext = view.context
            while (currentContext is ContextWrapper) {
                if (currentContext is Activity) {
                    break
                }
                currentContext = currentContext.baseContext
            }
            val activity = currentContext as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                
                val windowInsetsController = WindowCompat.getInsetsController(window, view)
                windowInsetsController.isAppearanceLightStatusBars = !darkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
