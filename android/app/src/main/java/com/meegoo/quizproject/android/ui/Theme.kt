package com.meegoo.quizproject.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.google.accompanist.insets.ExperimentalAnimatedInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val LocalExtraColors = staticCompositionLocalOf<ExtraColors> { error("No ExtraColors found") }
private val primaryLightColor = Color(0xffff9800)
private val primaryDarkColor = Color(0xffffab91)
private val primaryVariant = Color(0xffe64a19)

private val secondaryColor = Color(0xffffe082)
private val secondaryVariant = Color(0xffffab00)

private val darkErrorColor = Color(0xffe57373)
private val lightErrorColor = Color(0xff7f0000)

private val whiteText = Color(0xffffffff)
private val blackText = Color(0xff000000)

private val backgroundDarkColor = Color(0xff212121)
private val backgroundLightColor = Color(0xffffffff)
private val surfaceDarkColor = Color(0xff212121)
private val surfaceLightColor = Color(0xffF5F5F5)

@OptIn(ExperimentalAnimatedInsets::class)
@Composable
fun QuizMaterialTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()

    val darkColors = darkColors(
        primary = primaryDarkColor,
        primaryVariant = primaryVariant,
        secondary = secondaryColor,
        secondaryVariant = secondaryColor,
        background = backgroundDarkColor,
        surface = surfaceDarkColor,
        error = darkErrorColor,
        onPrimary = blackText,
        onSecondary = blackText,
        onBackground = whiteText,
        onError = darkErrorColor,
        onSurface = whiteText
    )
    val lightColors = lightColors(
        primary = primaryLightColor,
        primaryVariant = primaryVariant,
        secondary = secondaryColor,
        secondaryVariant = secondaryVariant,
        background = backgroundLightColor,
        surface = surfaceLightColor,
        error = lightErrorColor,
        onPrimary = blackText,
        onSecondary = blackText,
        onBackground = blackText,
        onError = lightErrorColor,
        onSurface = blackText
    )

    val lightTypography = Typography(
        h1 = MaterialTheme.typography.h1.merge(TextStyle(color = blackText)),
        h2 = MaterialTheme.typography.h2.merge(TextStyle(color = blackText)),
        h3 = MaterialTheme.typography.h3.merge(TextStyle(color = blackText)),
        h4 = MaterialTheme.typography.h4.merge(TextStyle(color = blackText)),
        h5 = MaterialTheme.typography.h5.merge(TextStyle(color = blackText)),
        h6 = MaterialTheme.typography.h6.merge(TextStyle(color = blackText)),
        subtitle1 = MaterialTheme.typography.subtitle1.merge(TextStyle(color = blackText)),
        subtitle2 = MaterialTheme.typography.subtitle2.merge(TextStyle(color = blackText)),
        body1 = MaterialTheme.typography.body1.merge(TextStyle(color = blackText)),
        body2 = MaterialTheme.typography.body2.merge(TextStyle(color = blackText)),
        button = MaterialTheme.typography.button.merge(TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)),
        caption = MaterialTheme.typography.caption.merge(TextStyle(color = blackText)),
        overline = MaterialTheme.typography.overline.merge(TextStyle(color = blackText))
    )


    val darkTypography = Typography(
        h1 = MaterialTheme.typography.h1.merge(TextStyle(color = whiteText)),
        h2 = MaterialTheme.typography.h2.merge(TextStyle(color = whiteText)),
        h3 = MaterialTheme.typography.h3.merge(TextStyle(color = whiteText)),
        h4 = MaterialTheme.typography.h4.merge(TextStyle(color = whiteText)),
        h5 = MaterialTheme.typography.h5.merge(TextStyle(color = whiteText)),
        h6 = MaterialTheme.typography.h6.merge(TextStyle(color = whiteText)),
        subtitle1 = MaterialTheme.typography.subtitle1.merge(TextStyle(color = whiteText)),
        subtitle2 = MaterialTheme.typography.subtitle2.merge(TextStyle(color = whiteText)),
        body1 = MaterialTheme.typography.body1.merge(TextStyle(color = whiteText)),
        body2 = MaterialTheme.typography.body2.merge(TextStyle(color = whiteText)),
        button = MaterialTheme.typography.button.merge(TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)),
        caption = MaterialTheme.typography.caption.merge(TextStyle(color = whiteText)),
        overline = MaterialTheme.typography.overline.merge(TextStyle(color = whiteText))
    )

//    val isDark = isSystemInDarkTheme()
    val isDark = true
    LaunchedEffect(true) {
        if (!isDark) {
            val systemColor = lerp(Color.Black, primaryLightColor, 0.8f)
            systemUiController.setStatusBarColor(systemColor)
            systemUiController.setNavigationBarColor(systemColor)
        }
    }

    val colors = if (isDark) {
        ExtraColors(
            green = Color(0xFF2E7D32),
            red = Color(0xFFC62828),
            yellow = Color(0xFF9E9D24),
            greenTranslucent = Color(0x802E7D32),
            redTranslucent = Color(0x80C62828),
            yellowTranslucent = Color(0x809E9D24),
            backgroundOverlay = Color(0x80000000),
            blue = Color(0xFF005cb2),
            purple = Color(0xFF9c27b0),
            divider = Color(0xFF000000),
            disabledText = LocalContentColor.current.copy(LocalContentAlpha.current).copy(ContentAlpha.disabled),
            onPrimaryColored = primaryDarkColor,
        )
    } else {
        ExtraColors(
            green = Color(0xFF2E7D32),
            red = Color(0xFFC62828),
            yellow = Color(0xFF9E9D24),
            greenTranslucent = Color(0x802E7D32),
            redTranslucent = Color(0x80C62828),
            yellowTranslucent = Color(0x809E9D24),
            backgroundOverlay = Color(0x80000000),
            blue = Color(0xFF303f9f),
            purple = Color(0xFF6a1b9a),
            divider = Color(0xffbbbbbb),
            disabledText = LocalContentColor.current.copy(LocalContentAlpha.current).copy(ContentAlpha.disabled),
            onPrimaryColored = blackText,
        )
    }
    val typography = Typography(
        button = MaterialTheme.typography.button.merge(TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))
    )

    CompositionLocalProvider(LocalExtraColors provides colors) {
        MaterialTheme(colors = if (isDark) darkColors else lightColors,
//            typography = if (isDark) darkTypography else lightTypography,
            typography = typography,
        ) {

            ProvideWindowInsets(windowInsetsAnimationsEnabled = true) {
                content()
            }
        }
    }
}

val MaterialTheme.extraColors: ExtraColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtraColors.current

data class ExtraColors(
    val green: Color,
    val red: Color,
    val yellow: Color,
    val greenTranslucent: Color,
    val redTranslucent: Color,
    val yellowTranslucent: Color,
    val backgroundOverlay: Color,
    val blue: Color,
    val purple: Color,
    val divider: Color,
    val disabledText: Color,
    val onPrimaryColored: Color,
)