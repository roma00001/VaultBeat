package com.vaultbeat.ui.nowplaying.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap

@Composable
fun MenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    textColor: Color,
    secondaryTextColor: Color,
    selectedTextColor: Color = textColor
) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        val alpha by animateFloatAsState(if (selected) 1f else 0.6f, label = "MenuItemAlpha")
        Text(
            text,
            style = TextStyle(
                fontSize = 13.sp,
                color = (if (selected) selectedTextColor else secondaryTextColor).copy(alpha = alpha),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
                .padding(vertical = 6.dp, horizontal = 4.dp)
        )
    }
}

data class ContrastTextColors(val primary: Color, val secondary: Color, val tertiary: Color)

@Composable
fun rememberContrastTextColors(albumArtModel: Any?): ContrastTextColors {
    val context = LocalContext.current
    val defaultColors = ContrastTextColors(
        primary = Color.White,
        secondary = Color(0xFFDDDDDD),
        tertiary = Color(0xFFBFBFBF)
    )
    val textColors = remember(albumArtModel) { mutableStateOf(defaultColors) }

    LaunchedEffect(albumArtModel) {
        if (albumArtModel == null) {
            textColors.value = defaultColors
            return@LaunchedEffect
        }

        val request = ImageRequest.Builder(context)
            .data(albumArtModel)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        val bitmap = when (result) {
            is SuccessResult -> result.image.toBitmap()
            else -> null
        }

        val averageColor = bitmap?.let { bitmapToAverageColor(it) }
        if (averageColor != null) {
            val isBackgroundLight = averageColor.luminance() > 0.5f
            textColors.value = if (isBackgroundLight) {
                ContrastTextColors(
                    primary = Color.Black,
                    secondary = Color.Black.copy(alpha = 0.88f),
                    tertiary = Color.Black.copy(alpha = 0.72f)
                )
            } else {
                ContrastTextColors(
                    primary = Color.White,
                    secondary = Color.White.copy(alpha = 0.88f),
                    tertiary = Color.White.copy(alpha = 0.72f)
                )
            }
        } else {
            textColors.value = defaultColors
        }
    }

    return textColors.value
}

private fun bitmapToAverageColor(bitmap: Bitmap): Color {
    val preview = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
    val pixel = preview.getPixel(0, 0)
    return Color(pixel)
}

@Composable
fun AnimatedSection(
    targetState: String,
    content: @Composable (String) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            if (targetState == "NOW_PLAYING") {
                (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400, easing = LinearOutSlowInEasing)))
                    .togetherWith(fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) + scaleOut(targetScale = 1.02f, animationSpec = tween(300, easing = FastOutLinearInEasing)))
            } else {
                (fadeIn(animationSpec = tween(400, easing = LinearOutSlowInEasing)) + slideInHorizontally(animationSpec = tween(400, easing = LinearOutSlowInEasing), initialOffsetX = { it / 20 }))
                    .togetherWith(fadeOut(animationSpec = tween(300, easing = FastOutLinearInEasing)) + slideOutHorizontally(animationSpec = tween(300, easing = FastOutLinearInEasing), targetOffsetX = { -it / 20 }))
            }
        },
        label = "FluidSectionTransition"
    ) { state ->
        content(state)
    }
}
