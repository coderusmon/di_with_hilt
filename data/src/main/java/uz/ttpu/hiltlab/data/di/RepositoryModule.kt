package uz.ttpu.hiltlab.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton
import uz.ttpu.hiltlab.data.BuildConfig
import uz.ttpu.hiltlab.data.logging.LogcatLogger
import uz.ttpu.hiltlab.data.logging.Logger
import uz.ttpu.hiltlab.data.repository.DefaultUserRepository
import uz.ttpu.hiltlab.data.repository.FakeNoteRepository
import uz.ttpu.hiltlab.data.repository.NoteRepository
import uz.ttpu.hiltlab.data.repository.RoomNoteRepository
import uz.ttpu.hiltlab.data.repository.UserRepository

/**
 * Q11: an interface cannot be constructed, so @Binds tells Hilt which
 * implementation to use. Switching LogcatLogger -> FileLogger is one line.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindLogger(impl: LogcatLogger): Logger

    @Binds
    abstract fun bindUserRepository(impl: DefaultUserRepository): UserRepository
}

/**
 * Q23: the note repository is chosen at build time.
 * `-PuseFakeNotes=true` runs the app against the fake without Room.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNoteRepository(
        real: Provider<RoomNoteRepository>,
        fake: Provider<FakeNoteRepository>,
    ): NoteRepository = if (BuildConfig.USE_FAKE_NOTES) fake.get() else real.get()
}
