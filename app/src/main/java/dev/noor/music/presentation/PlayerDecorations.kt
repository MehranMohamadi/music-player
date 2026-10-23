package dev.noor.music.presentation

import android.icu.text.BreakIterator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import dev.noor.music.common.*
import dev.noor.music.domain.SongReaction
import java.util.Locale

val ReactionEmoji = listOf("🔥", "❤️", "😍", "🥹", "😎", "💃", "🥳", "🌙", "💔", "✨")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionArtwork(
    artwork: String?,
    title: String,
    reactions: List<SongReaction>,
    enabled: Boolean,
    onAdd: (String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val addDescription = tr("Add emoji to cover", "افزودن ایموجی به کاور")
    var picker by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<SongReaction?>(null) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            Modifier.widthIn(max = 440.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Art(artwork, title, Modifier.fillMaxSize(), round = 16.dp, description = title)
            reactions.forEach { reaction ->
                key(reaction.id) {
                    val fresh = remember {
                        System.currentTimeMillis() - reaction.createdAt in 0..1500
                    }
                    val entrance = remember { Animatable(if (fresh) 0f else 1f) }
                    LaunchedEffect(Unit) {
                        if (fresh)
                            entrance.animateTo(
                                1f,
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                    }
                    Box(
                        Modifier.offset(
                                x = (maxWidth - 48.dp) * reaction.x,
                                y = (maxHeight - 96.dp) * reaction.y,
                            )
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = entrance.value
                                scaleY = entrance.value
                                alpha = entrance.value.coerceIn(0f, 1f)
                                translationY = (1f - entrance.value) * -100f
                                rotationZ = reaction.rotation + (1f - entrance.value) * 35f
                            }
                            .clickable { removing = reaction }
                            .semantics { contentDescription = "$title ${reaction.emoji}" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(reaction.emoji, fontSize = 32.sp)
                    }
                }
            }
            FilledTonalButton(
                onClick = { picker = true },
                enabled = enabled,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.Black.copy(alpha = .65f),
                        contentColor = Color.White,
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    "☺ +",
                    fontSize = 20.sp,
                    modifier =
                        Modifier.semantics {
                            contentDescription = addDescription
                        },
                )
            }
        }
    }
    if (picker) {
        var custom by remember { mutableStateOf("") }
        val valid = remember(custom) { isSingleEmoji(custom.trim()) }
        ModalBottomSheet(onDismissRequest = { picker = false }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(20.dp)) {
                Text(
                    tr("How does this song feel?", "با این آهنگ چه حسی داری؟"),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    tr(
                        "Tap as often as you like. Saved on this phone.",
                        "هر چند بار که دوست داری بزن؛ روی همین گوشی ذخیره می‌شود.",
                    ),
                    Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                ReactionEmoji.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { emoji ->
                            TextButton(
                                onClick = { onAdd(emoji) },
                                modifier = Modifier.size(56.dp),
                            ) {
                                Text(emoji, fontSize = 28.sp)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        custom,
                        { custom = it },
                        Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(tr("Your own emoji", "ایموجی دلخواه")) },
                        isError = custom.isNotBlank() && !valid,
                        supportingText = {
                            Text(
                                tr(
                                    "Choose one emoji from your keyboard",
                                    "یک ایموجی از کیبورد انتخاب کن",
                                )
                            )
                        },
                    )
                    TextButton(onClick = { onAdd(custom.trim()) }, enabled = valid) {
                        Text(tr("Add", "افزودن"))
                    }
                }
                TextButton(onClick = { picker = false }, modifier = Modifier.align(Alignment.End)) {
                    Text(tr("Done", "تمام"))
                }
            }
        }
    }
    removing?.let { reaction ->
        AlertDialog(
            onDismissRequest = { removing = null },
            text = {
                Text(
                    tr(
                        "Remove this ${reaction.emoji} from the cover?",
                        "این ${reaction.emoji} از کاور حذف شود؟",
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(reaction.id)
                        removing = null
                    }
                ) {
                    Text(tr("Remove", "حذف"))
                }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text(tr("Keep", "نگه داشتن")) }
            },
        )
    }
}

internal fun isSingleEmoji(text: String): Boolean {
    if (text.isBlank()) return false
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
    iterator.first()
    if (iterator.next() != text.length || iterator.next() != BreakIterator.DONE) return false
    return text.codePoints().anyMatch {
        android.icu.lang.UCharacter.hasBinaryProperty(
            it,
            android.icu.lang.UProperty.EMOJI_PRESENTATION,
        ) ||
            it in 0x2600..0x27ff ||
            it == 0x2764 ||
            it == 0x20e3 ||
            it == 0x00a9 ||
            it == 0x00ae ||
            it == 0x2122
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveformSeekBar(
    value: Float,
    duration: Float,
    waveform: List<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    val description = tr("Playback position", "موقعیت پخش")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Slider(
            value = value.coerceIn(0f, duration.coerceAtLeast(1f)),
            onValueChange = onChange,
            onValueChangeFinished = onFinished,
            valueRange = 0f..duration.coerceAtLeast(1f),
            enabled = duration > 0,
            modifier =
                Modifier.fillMaxWidth().height(48.dp).semantics {
                    contentDescription = description
                },
            thumb = {
                Box(
                    Modifier.size(width = 3.dp, height = 36.dp).background(Color.White, CircleShape)
                )
            },
            track = { slider ->
                Canvas(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(6.dp))) {
                    drawRoundRect(Color(0xFF454545), cornerRadius = CornerRadius(6.dp.toPx()))
                    val progress = (slider.value / duration.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    fun bars(color: Color) {
                        if (waveform.isEmpty()) {
                            drawRect(
                                color,
                                Offset(0f, size.height / 2 - 1.dp.toPx()),
                                Size(size.width, 2.dp.toPx()),
                            )
                        } else {
                            val step = size.width / waveform.size
                            waveform.forEachIndexed { index, amplitude ->
                                val h = 2.dp.toPx() + amplitude * (size.height - 4.dp.toPx())
                                drawRoundRect(
                                    color,
                                    Offset(index * step, (size.height - h) / 2),
                                    Size((step * .65f).coerceAtLeast(1f), h),
                                    CornerRadius(1.dp.toPx()),
                                )
                            }
                        }
                    }
                    bars(Color(0xFFAAAAAA))
                    clipRect(right = size.width * progress) {
                        drawRect(Green.copy(alpha = .18f))
                        bars(Green)
                    }
                }
            },
        )
    }
}
