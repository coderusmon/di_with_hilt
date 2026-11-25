package uz.ttpu.hiltlab.data.model

data class User(
    val id: Long,
    val login: String,
    val avatarUrl: String,
)

data class Note(
    val id: Long = 0L,
    val title: String,
    val body: String,
    val updatedAt: Long = System.currentTimeMillis(),
)
