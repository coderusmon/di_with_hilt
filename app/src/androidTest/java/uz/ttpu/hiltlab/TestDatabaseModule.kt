package uz.ttpu.hiltlab

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import uz.ttpu.hiltlab.data.di.DatabaseModule
import uz.ttpu.hiltlab.data.local.AppDatabase
import uz.ttpu.hiltlab.data.local.NoteDao
import uz.ttpu.hiltlab.data.local.UserDao

/**
 * Q27: @TestInstallIn replaces DatabaseModule in every test,
 * so tests use an in-memory Room database instead of hiltlab.db.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()
}
