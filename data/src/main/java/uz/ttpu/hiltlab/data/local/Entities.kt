package uz.ttpu.hiltlab.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import uz.ttpu.hiltlab.data.model.Note
import uz.ttpu.hiltlab.data.model.User

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val login: String,
    val avatarUrl: String,
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

fun UserEntity.toModel() = User(id = id, login = login, avatarUrl = avatarUrl)

fun User.toEntity() = UserEntity(id = id, login = login, avatarUrl = avatarUrl)

fun NoteEntity.toModel() = Note(id = id, title = title, body = body, updatedAt = updatedAt)

fun Note.toEntity() = NoteEntity(id = id, title = title, body = body, updatedAt = updatedAt)
