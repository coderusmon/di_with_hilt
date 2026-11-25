package uz.ttpu.hiltlab.data.repository

import uz.ttpu.hiltlab.data.local.UserDao
import uz.ttpu.hiltlab.data.local.toEntity
import uz.ttpu.hiltlab.data.local.toModel
import uz.ttpu.hiltlab.data.model.User
import uz.ttpu.hiltlab.data.remote.GitHubApi
import uz.ttpu.hiltlab.data.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

interface UserRepository {
    suspend fun users(forceRefresh: Boolean = false): List<User>
}

@Singleton
class DefaultUserRepository @Inject constructor(
    private val api: GitHubApi,
    private val dao: UserDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : UserRepository {

    override suspend fun users(forceRefresh: Boolean): List<User> = withContext(io) {
        val cached = dao.all()
        if (!forceRefresh && cached.isNotEmpty()) {
            return@withContext cached.map { it.toModel() }
        }

        runCatching { api.users() }
            .onSuccess { remote ->
                dao.clear()
                dao.upsertAll(remote.map { User(it.id, it.login, it.avatarUrl).toEntity() })
            }
            .getOrElse { error ->
                if (cached.isEmpty()) throw error
            }

        dao.all().map { it.toModel() }
    }
}
