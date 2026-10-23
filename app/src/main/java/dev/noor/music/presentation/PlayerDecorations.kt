package dev.noor.music.presentation

import android.icu.text.BreakIterator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import dev.noor.music.common.*
import dev.noor.music.domain.SongReaction
import java.util.Locale
import kotlin.math.sin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val ReactionEmoji = listOf("❤️", "🔥", "😍", "🥹", "😎", "💃", "🥳", "🌙", "💔", "✨")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionArtwork(
    artwork: String?,
    title: String,
    reactions: List<SongReaction>,
    enabled: Boolean,
    onAdd: (String, Float, Float) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val selectDescription = tr("Choose emoji", "انتخاب ایموجی")
    val placeDescription = tr("Place selected emoji on cover", "گذاشتن ایموجی انتخاب‌شده روی کاور")
    val removeDescription = tr("Remove emoji", "حذف ایموجی")
    val currentReactions by rememberUpdatedState(reactions)
    val placeReaction by rememberUpdatedState(onAdd)
    var selectedEmoji by rememberSaveable { mutableStateOf("❤️") }
    var picker by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<SongReaction?>(null) }
    Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            BoxWithConstraints(
                Modifier.fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(enabled, selectedEmoji) {
                        detectTapGestures(
                            onTap = { point ->
                                if (enabled && size.width > 0 && size.height > 0) {
                                    placeReaction(
                                        selectedEmoji,
                                        (point.x / size.width).coerceIn(0f, 1f),
                                        (point.y / size.height).coerceIn(0f, 1f),
                                    )
                                }
                            },
                            onLongPress = { point ->
                                val diameter = 48.dp.toPx()
                                removing =
                                    currentReactions.asReversed().firstOrNull { reaction ->
                                        val (x, y) =
                                            reactionTopLeft(
                                                reaction,
                                                size.width.toFloat(),
                                                size.height.toFloat(),
                                                diameter,
                                            )
                                        point.x in x..(x + diameter) && point.y in y..(y + diameter)
                                    }
                            },
                        )
                    }
                    .semantics {
                        contentDescription = title
                        onClick(label = placeDescription) {
                            if (enabled) onAdd(selectedEmoji, .5f, .5f)
                            enabled
                        }
                    }
            ) {
                Art(artwork, title, Modifier.fillMaxSize(), round = 16.dp)
                reactions.forEach { reaction ->
                    key(reaction.id) {
                        val fresh = remember {
                            System.currentTimeMillis() - reaction.createdAt in 0..1500
                        }
                        val entrance = remember { Animatable(if (fresh) 0f else 1f) }
                        val wiggle = remember { Animatable(if (fresh) 0f else 1f) }
                        LaunchedEffect(Unit) {
                            if (fresh)
                                coroutineScope {
                                    launch {
                                        entrance.animateTo(
                                            1f,
                                            spring(
                                                dampingRatio = .42f,
                                                stiffness = Spring.StiffnessLow,
                                            ),
                                        )
                                    }
                                    launch {
                                        delay(150)
                                        wiggle.animateTo(1f, tween(1100))
                                    }
                                }
                        }
                        val (left, top) =
                            reactionTopLeft(reaction, maxWidth.value, maxHeight.value, 48f)
                        Box(
                            Modifier.offset(left.dp, top.dp)
                                .size(48.dp)
                                .graphicsLayer {
                                    scaleX = entrance.value
                                    scaleY = entrance.value
                                    alpha = entrance.value.coerceIn(0f, 1f)
                                    translationY = (1f - entrance.value) * -48.dp.toPx()
                                    rotationZ =
                                        reaction.rotation +
                                            (1f - entrance.value) * 35f +
                                            sin(wiggle.value * 4f * Math.PI.toFloat()) *
                                                (1f - wiggle.value) *
                                                14f
                                }
                                .semantics {
                                    contentDescription = reaction.emoji
                                    onLongClick(label = removeDescription) {
                                        removing = reaction
                                        true
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(reaction.emoji, fontSize = 32.sp)
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                tr(
                    "Tap the cover to place; hold an emoji to remove.",
                    "برای افزودن، کاور را بزن؛ برای حذف، ایموجی را نگه دار.",
                ),
                Modifier.weight(1f).padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = .7f),
            )
            FilledTonalButton(
                onClick = { picker = true },
                enabled = enabled,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.semantics { contentDescription = selectDescription },
            ) {
                Text("$selectedEmoji ▾", fontSize = 24.sp)
            }
        }
    }
    if (picker) {
        var custom by remember { mutableStateOf("") }
        val valid = remember(custom) { isSingleEmoji(custom.trim()) }
        ModalBottomSheet(onDismissRequest = { picker = false }) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(20.dp)) {
                Text(
                    tr("Choose an emoji", "یک ایموجی انتخاب کن"),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    tr(
                        "Choose here, then tap anywhere on the cover. Heart is selected by default.",
                        "ایموجی را انتخاب کن، سپس روی کاور بزن. قلب انتخاب پیش‌فرض است.",
                    ),
                    Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                ReactionEmoji.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { emoji ->
                            TextButton(
                                onClick = {
                                    selectedEmoji = emoji
                                    picker = false
                                },
                                colors =
                                    ButtonDefaults.textButtonColors(
                                        containerColor =
                                            if (selectedEmoji == emoji) Green.copy(alpha = .2f)
                                            else Color.Transparent
                                    ),
                                modifier =
                                    Modifier.size(56.dp).semantics {
                                        selected = selectedEmoji == emoji
                                    },
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
                    TextButton(
                        onClick = {
                            selectedEmoji = custom.trim()
                            picker = false
                        },
                        enabled = valid,
                    ) {
                        Text(tr("Select", "انتخاب"))
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
