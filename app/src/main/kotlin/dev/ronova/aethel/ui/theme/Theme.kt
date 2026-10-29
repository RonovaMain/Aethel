package dev.ronova.aethel.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.ronova.aethel.data.PaletteId
import dev.ronova.aethel.data.ThemeMode

/** Набор базовых цветов палитры; остальные роли выводятся из них. */
private class Spec(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val surface: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
)

private class Palette(val light: Spec, val dark: Spec)

private val AethelPalette = Palette(
    light = Spec(
        primary = 0xFF5B4FC9, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFE4DFFF, onPrimaryContainer = 0xFF170F6B,
        secondary = 0xFF5D5C72, secondaryContainer = 0xFFE3E0F9, onSecondaryContainer = 0xFF1A1A2C,
        tertiary = 0xFF0B6AA8, tertiaryContainer = 0xFFCDE5FF, onTertiaryContainer = 0xFF001D33,
        surface = 0xFFFCF8FF, onSurface = 0xFF1B1B21, onSurfaceVariant = 0xFF47464F, outline = 0xFF787680,
    ),
    dark = Spec(
        primary = 0xFFC6BFFF, onPrimary = 0xFF2A1E9A,
        primaryContainer = 0xFF4236B0, onPrimaryContainer = 0xFFE4DFFF,
        secondary = 0xFFC7C4DD, secondaryContainer = 0xFF454559, onSecondaryContainer = 0xFFE3E0F9,
        tertiary = 0xFF98CBFF, tertiaryContainer = 0xFF004A77, onTertiaryContainer = 0xFFCDE5FF,
        surface = 0xFF131318, onSurface = 0xFFE5E1E9, onSurfaceVariant = 0xFFC9C5D0, outline = 0xFF928F99,
    ),
)

private val SakuraPalette = Palette(
    light = Spec(
        primary = 0xFFB5527B, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFFFD9E6, onPrimaryContainer = 0xFF3D0020,
        secondary = 0xFF74565F, secondaryContainer = 0xFFFFD9E3, onSecondaryContainer = 0xFF2B151C,
        tertiary = 0xFF7D5260, tertiaryContainer = 0xFFFFD8E4, onTertiaryContainer = 0xFF31111D,
        surface = 0xFFFFF8F9, onSurface = 0xFF201A1C, onSurfaceVariant = 0xFF514347, outline = 0xFF837377,
    ),
    dark = Spec(
        primary = 0xFFFFB0CC, onPrimary = 0xFF5E1138,
        primaryContainer = 0xFF7B2B50, onPrimaryContainer = 0xFFFFD9E6,
        secondary = 0xFFE3BDC9, secondaryContainer = 0xFF5B3F48, onSecondaryContainer = 0xFFFFD9E3,
        tertiary = 0xFFEFB8C8, tertiaryContainer = 0xFF633B48, onTertiaryContainer = 0xFFFFD8E4,
        surface = 0xFF211A1D, onSurface = 0xFFEDDFE2, onSurfaceVariant = 0xFFD6C2C7, outline = 0xFF9F8C91,
    ),
)

private val OceanPalette = Palette(
    light = Spec(
        primary = 0xFF1F6FB2, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFD1E4FF, onPrimaryContainer = 0xFF001D36,
        secondary = 0xFF535F70, secondaryContainer = 0xFFD7E3F8, onSecondaryContainer = 0xFF101C2B,
        tertiary = 0xFF00696B, tertiaryContainer = 0xFFB3F0EE, onTertiaryContainer = 0xFF002020,
        surface = 0xFFF8F9FF, onSurface = 0xFF191C20, onSurfaceVariant = 0xFF43474E, outline = 0xFF73777F,
    ),
    dark = Spec(
        primary = 0xFFA0CAFF, onPrimary = 0xFF00325A,
        primaryContainer = 0xFF00497D, onPrimaryContainer = 0xFFD1E4FF,
        secondary = 0xFFBBC7DB, secondaryContainer = 0xFF3B4858, onSecondaryContainer = 0xFFD7E3F8,
        tertiary = 0xFF80D5D4, tertiaryContainer = 0xFF004F50, onTertiaryContainer = 0xFFB3F0EE,
        surface = 0xFF101418, onSurface = 0xFFE0E2E8, onSurfaceVariant = 0xFFC3C7CF, outline = 0xFF8D9199,
    ),
)

private val MintPalette = Palette(
    light = Spec(
        primary = 0xFF2E6C4E, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFB1F1CD, onPrimaryContainer = 0xFF002113,
        secondary = 0xFF4E6355, secondaryContainer = 0xFFD0E8D6, onSecondaryContainer = 0xFF0B1F14,
        tertiary = 0xFF3A6471, tertiaryContainer = 0xFFBEE9F8, onTertiaryContainer = 0xFF001F28,
        surface = 0xFFF6FBF4, onSurface = 0xFF181D19, onSurfaceVariant = 0xFF414942, outline = 0xFF717972,
    ),
    dark = Spec(
        primary = 0xFF96D5B2, onPrimary = 0xFF003823,
        primaryContainer = 0xFF0F5137, onPrimaryContainer = 0xFFB1F1CD,
        secondary = 0xFFB4CCBA, secondaryContainer = 0xFF374B3E, onSecondaryContainer = 0xFFD0E8D6,
        tertiary = 0xFFA3CDDC, tertiaryContainer = 0xFF214C59, onTertiaryContainer = 0xFFBEE9F8,
        surface = 0xFF0F1511, onSurface = 0xFFDFE4DD, onSurfaceVariant = 0xFFC0C9C0, outline = 0xFF8A938A,
    ),
)

private fun paletteOf(id: PaletteId): Palette = when (id) {
    PaletteId.SAKURA -> SakuraPalette
    PaletteId.OCEAN -> OceanPalette
    PaletteId.MINT -> MintPalette
    else -> AethelPalette
}

private fun Spec.toScheme(dark: Boolean): ColorScheme {
    val primaryC = Color(primary)
    val surfaceC = Color(surface)
    val secContainer = Color(secondaryContainer)
    val onPrimaryC = Color(onPrimary)

    val lowest: Color
    val low: Color
    val container: Color
    val high: Color
    val highest: Color
    val dim: Color
    val bright: Color
    if (dark) {
        lowest = lerp(surfaceC, Color.Black, 0.30f)
        low = lerp(surfaceC, primaryC, 0.05f)
        container = lerp(surfaceC, primaryC, 0.08f)
        high = lerp(surfaceC, primaryC, 0.11f)
        highest = lerp(surfaceC, primaryC, 0.15f)
        dim = surfaceC
        bright = lerp(surfaceC, primaryC, 0.18f)
    } else {
        lowest = lerp(surfaceC, Color.White, 0.60f)
        low = lerp(surfaceC, primaryC, 0.03f)
        container = lerp(surfaceC, primaryC, 0.06f)
        high = lerp(surfaceC, primaryC, 0.09f)
        highest = lerp(surfaceC, primaryC, 0.12f)
        dim = lerp(surfaceC, Color.Black, 0.08f)
        bright = surfaceC
    }
    val surfaceVariantC = lerp(surfaceC, secContainer, 0.55f)
    val outlineVariantC = lerp(Color(outline), surfaceC, 0.60f)

    return if (dark) {
        darkColorScheme(
            primary = primaryC,
            onPrimary = onPrimaryC,
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(onPrimaryContainer),
            inversePrimary = Color(primaryContainer),
            secondary = Color(secondary),
            onSecondary = onPrimaryC,
            secondaryContainer = secContainer,
            onSecondaryContainer = Color(onSecondaryContainer),
            tertiary = Color(tertiary),
            onTertiary = onPrimaryC,
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(onTertiaryContainer),
            background = surfaceC,
            onBackground = Color(onSurface),
            surface = surfaceC,
            onSurface = Color(onSurface),
            surfaceVariant = surfaceVariantC,
            onSurfaceVariant = Color(onSurfaceVariant),
            surfaceTint = primaryC,
            inverseSurface = Color(onSurface),
            inverseOnSurface = surfaceC,
            outline = Color(outline),
            outlineVariant = outlineVariantC,
            surfaceBright = bright,
            surfaceDim = dim,
            surfaceContainerLowest = lowest,
            surfaceContainerLow = low,
            surfaceContainer = container,
            surfaceContainerHigh = high,
            surfaceContainerHighest = highest,
        )
    } else {
        lightColorScheme(
            primary = primaryC,
            onPrimary = onPrimaryC,
            primaryContainer = Color(primaryContainer),
            onPrimaryContainer = Color(onPrimaryContainer),
            inversePrimary = Color(primaryContainer),
            secondary = Color(secondary),
            onSecondary = onPrimaryC,
            secondaryContainer = secContainer,
            onSecondaryContainer = Color(onSecondaryContainer),
            tertiary = Color(tertiary),
            onTertiary = onPrimaryC,
            tertiaryContainer = Color(tertiaryContainer),
            onTertiaryContainer = Color(onTertiaryContainer),
            background = surfaceC,
            onBackground = Color(onSurface),
            surface = surfaceC,
            onSurface = Color(onSurface),
            surfaceVariant = surfaceVariantC,
            onSurfaceVariant = Color(onSurfaceVariant),
            surfaceTint = primaryC,
            inverseSurface = Color(onSurface),
            inverseOnSurface = surfaceC,
            outline = Color(outline),
            outlineVariant = outlineVariantC,
            surfaceBright = bright,
            surfaceDim = dim,
            surfaceContainerLowest = lowest,
            surfaceContainerLow = low,
            surfaceContainer = container,
            surfaceContainerHigh = high,
            surfaceContainerHighest = highest,
        )
    }
}

/** Цвет для кружка-образца палитры в настройках. */
fun paletteSwatch(id: PaletteId, dark: Boolean): Color {
    val spec = paletteOf(id).let { if (dark) it.dark else it.light }
    return Color(spec.primary)
}

fun paletteSwatchSecondary(id: PaletteId, dark: Boolean): Color {
    val spec = paletteOf(id).let { if (dark) it.dark else it.light }
    return Color(spec.primaryContainer)
}

val dynamicColorsSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun AethelTheme(
    mode: ThemeMode,
    palette: PaletteId,
    amoled: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current

    var scheme: ColorScheme = if (palette == PaletteId.DYNAMIC && dynamicColorsSupported) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val id = if (palette == PaletteId.DYNAMIC) PaletteId.AETHEL else palette
        val p = paletteOf(id)
        (if (dark) p.dark else p.light).toScheme(dark)
    }

    if (dark && amoled) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF101010),
            surfaceContainerHigh = Color(0xFF171717),
            surfaceContainerHighest = Color(0xFF1E1E1E),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
