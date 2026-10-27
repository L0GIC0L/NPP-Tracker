package io.github.l0gic0l.npptracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.l0gic0l.npptracker.data.PowerStatus

// Fallback scheme for Android 11 and lower, generated from a blue seed (#0061A4).
private val LightColors = lightColorScheme(
    primary = Color(0xFF0061A4), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF), onPrimaryContainer = Color(0xFF001D36),
    inversePrimary = Color(0xFF9ECAFF),
    secondary = Color(0xFF535F70), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7), onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF), onTertiaryContainer = Color(0xFF251431),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FC), onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF9F9FC), onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB), onSurfaceVariant = Color(0xFF43474E),
    surfaceTint = Color(0xFF0061A4),
    inverseSurface = Color(0xFF2F3033), inverseOnSurface = Color(0xFFF1F0F4),
    outline = Color(0xFF73777F), outlineVariant = Color(0xFFC3C7CF),
    surfaceBright = Color(0xFFF9F9FC), surfaceDim = Color(0xFFD9DADD),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF3F3F6),
    surfaceContainer = Color(0xFFEDEEF1), surfaceContainerHigh = Color(0xFFE7E8EB),
    surfaceContainerHighest = Color(0xFFE2E2E5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF), onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D), onPrimaryContainer = Color(0xFFD1E4FF),
    inversePrimary = Color(0xFF0061A4),
    secondary = Color(0xFFBBC7DB), onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858), onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4), onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F), onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111316), onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF111316), onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E), onSurfaceVariant = Color(0xFFC3C7CF),
    surfaceTint = Color(0xFF9ECAFF),
    inverseSurface = Color(0xFFE2E2E6), inverseOnSurface = Color(0xFF2F3033),
    outline = Color(0xFF8D9199), outlineVariant = Color(0xFF43474E),
    surfaceBright = Color(0xFF37393C), surfaceDim = Color(0xFF111316),
    surfaceContainerLowest = Color(0xFF0C0E11), surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2022), surfaceContainerHigh = Color(0xFF282A2D),
    surfaceContainerHighest = Color(0xFF333538),
)

@Composable
fun NppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** The color used for a power status everywhere in the app (rings, chips, charts). */
fun ColorScheme.statusColor(status: PowerStatus): Color = when (status) {
    PowerStatus.FULL -> primary
    PowerStatus.REDUCED -> tertiary
    PowerStatus.OFFLINE -> error
}
