package dev.noor.music.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.noor.music.common.*
import dev.noor.music.domain.*

@Composable
fun EmojiGroupsScreen(songs: List<Song>, reactions: List<SongReaction>, onOpen: (String) -> Unit) {
    val groups =
        remember(songs, reactions) {
            val visible = songs.mapTo(hashSetOf()) { it.uri }
            reactions
                .filter { it.songUri in visible }
                .groupBy { it.emoji }
                .mapValues { (_, entries) -> entries.map { it.songUri }.distinct().size }
                .toList()
                .sortedByDescending { it.second }
        }
    LazyVerticalGrid(
        GridCells.Adaptive(140.dp),
        contentPadding = PaddingValues(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(
                    tr("Your moods", "حال‌وهوای آهنگ‌ها"),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    tr(
                        "Songs grouped by the emoji on their covers",
                        "آهنگ‌ها بر اساس ایموجی‌های روی کاور",
                    ),
                    Modifier.padding(vertical = 8.dp),
                )
            }
        }
        if (groups.isEmpty())
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    tr(
                        "Open a song and add emoji to its cover to start.",
                        "یک آهنگ باز کن و به کاورش ایموجی اضافه کن.",
                    ),
                    Modifier.padding(vertical = 40.dp),
                )
            }
        items(groups, key = { it.first }) { (emoji, count) ->
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    Modifier.clickable { onOpen(emoji) }.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(emoji, fontSize = 42.sp)
                    Text("$count " + tr("songs", "آهنگ"), Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
