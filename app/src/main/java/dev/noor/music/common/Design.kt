package dev.noor.music.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import dev.noor.music.common.icons.rounded.*
import java.util.Locale

val Green = Color(0xFF1DB954)
val LocalPersian = staticCompositionLocalOf { false }

@Composable
fun tr(english: String, persian: String) = if (LocalPersian.current) persian else english

@Composable
fun NoorTheme(light: Boolean = false, content: @Composable () -> Unit) {
    val colors =
        if (light)
            lightColorScheme(
                primary = Color(0xFF087F3A),
                primaryContainer = Color(0xFFC4F1D2),
                onPrimaryContainer = Color(0xFF10351F),
                secondary = Color(0xFF087F3A),
                secondaryContainer = Color(0xFFC4F1D2),
                onSecondaryContainer = Color(0xFF10351F),
                onPrimary = Color.White,
                background = Color(0xFFF7F7F5),
                surface = Color(0xFFF7F7F5),
                surfaceContainer = Color(0xFFEBEDE8),
            )
        else
            darkColorScheme(
                primary = Green,
                primaryContainer = Color(0xFF193E2A),
                onPrimaryContainer = Color(0xFFC3F7D5),
                secondary = Green,
                secondaryContainer = Color(0xFF244335),
                onSecondaryContainer = Color.White,
                onPrimary = Color(0xFF07180D),
                background = Color(0xFF121212),
                surface = Color(0xFF121212),
                surfaceContainer = Color(0xFF181818),
                surfaceContainerHigh = Color(0xFF242424),
                onSurface = Color(0xFFF5F5F1),
                onSurfaceVariant = Color(0xFFAAAAA5),
            )
    MaterialTheme(
        colorScheme = colors,
        typography =
            Typography(
                headlineLarge =
                    Typography()
                        .headlineLarge
                        .copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
                headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.Bold),
                titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold),
                titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
            ),
        content = content,
    )
}

@Composable
fun Art(
    model: Any?,
    seed: String,
    modifier: Modifier = Modifier,
    round: Dp = 12.dp,
    description: String? = null,
) {
    val hue = ((seed.hashCode().toLong() and 0x7fffffff) % 360).toFloat()
    Box(
        modifier
            .clip(RoundedCornerShape(round))
            .background(
                Brush.linearGradient(
                    listOf(Color.hsl(hue, 0.4f, 0.32f), Color.hsl((hue + 45) % 360, 0.3f, 0.12f))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        val fallback: @Composable () -> Unit = {
            Icon(
                Icons.Rounded.GraphicEq,
                description,
                Modifier.fillMaxSize(0.38f),
                tint = Color.White.copy(alpha = .8f),
            )
        }
        if (model == null) fallback()
        else
            SubcomposeAsyncImage(
                model =
                    ImageRequest.Builder(LocalContext.current).data(model).crossfade(180).build(),
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = { fallback() },
                error = { fallback() },
            )
    }
}

@Composable
fun ActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
    ) {
        Icon(icon, label, tint = if (enabled) tint else tint.copy(alpha = .3f))
    }
}

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
fun TrackText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color =
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun time(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return if (seconds >= 3600)
        String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    else String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}

fun size(bytes: Long) = String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0)
