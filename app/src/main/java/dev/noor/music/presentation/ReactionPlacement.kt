package dev.noor.music.presentation

import dev.noor.music.domain.SongReaction

/** New stickers are centered on the tap; legacy stickers retain their original layout. */
internal fun reactionTopLeft(
    reaction: SongReaction,
    width: Float,
    height: Float,
    diameter: Float,
): Pair<Float, Float> =
    if (reaction.centered) {
        (width * reaction.x - diameter / 2).coerceIn(0f, (width - diameter).coerceAtLeast(0f)) to
            (height * reaction.y - diameter / 2).coerceIn(0f, (height - diameter).coerceAtLeast(0f))
    } else {
        (width - diameter).coerceAtLeast(0f) * reaction.x to
            (height - diameter * 2).coerceAtLeast(0f) * reaction.y
    }
