package dev.noor.music.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.noor.music.domain.SongReaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class ReactionMigrationTest {
    @Test
    fun upgradeAndReopenPreserveExistingLibraryAndNewReactions() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reaction-upgrade-test.db"
        context.deleteDatabase(name)
        val file = context.getDatabasePath(name).apply { parentFile!!.mkdirs() }
        val schema =
            javaClass
                .getResourceAsStream("/dev.noor.music.database.MusicDatabase/1.json")!!
                .bufferedReader()
                .use { JSONObject(it.readText()).getJSONObject("database") }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null)
                    for (j in 0 until indices.length()) {
                        old.execSQL(
                            indices
                                .getJSONObject(j)
                                .getString("createSql")
                                .replace("\${TABLE_NAME}", table)
                        )
                    }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL(
                "INSERT INTO songs VALUES ('song:1', 1, 'Old song', 'Artist', 'Album', 1, 180000, 1, 400, 'audio/mpeg', 'song.mp3', 'Music/', 1, 7, 1234, 99)"
            )
            old.execSQL("INSERT INTO collections VALUES (1, 'My collection', NULL)")
            old.execSQL("INSERT INTO members VALUES (1, 'song:1', 0)")
            old.version = 1
        }
        fun open() =
            Room.databaseBuilder(context, MusicDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .allowMainThreadQueries()
                .build()
        try {
            val reaction =
                SongReaction(songUri = "song:1", emoji = "🥹", x = .25f, y = .75f, rotation = 9f)
            var savedId = 0L
            val migrated = open()
            try {
                val db = migrated
                val song = db.music().snapshot().single()
                assertTrue(song.favorite)
                assertEquals(7, song.playCount)
                assertEquals(99L, song.revision)
                assertEquals(listOf("song:1"), db.music().members(1))
                savedId = db.music().addReaction(reaction)
            } finally {
                migrated.close()
            }
            val reopened = open()
            try {
                val db = reopened
                assertEquals(listOf(reaction.copy(id = savedId)), db.music().reactions().first())
                assertEquals("My collection", db.music().collections().first().single().name)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
