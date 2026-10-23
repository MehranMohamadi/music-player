package dev.noor.music.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.noor.music.database.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object Injection {
    @Provides
    @Singleton
    fun database(@ApplicationContext c: Context): MusicDatabase =
        Room.databaseBuilder(c, MusicDatabase::class.java, "noor.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun collectionStore(repo: MusicRepository): dev.noor.music.domain.CollectionStore = repo

    @Provides fun dao(db: MusicDatabase) = db.music()
}
