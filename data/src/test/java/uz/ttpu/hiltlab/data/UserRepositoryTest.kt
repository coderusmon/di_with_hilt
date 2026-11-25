package uz.ttpu.hiltlab.data

import com.google.gson.GsonBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uz.ttpu.hiltlab.data.local.UserDao
import uz.ttpu.hiltlab.data.local.UserEntity
import uz.ttpu.hiltlab.data.remote.GitHubApi
import uz.ttpu.hiltlab.data.repository.DefaultUserRepository

class UserRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var dao: InMemoryUserDao
    private lateinit var repository: DefaultUserRepository

    private class InMemoryUserDao : UserDao {
        var rows = mutableListOf<UserEntity>()
        override suspend fun all(): List<UserEntity> = rows.toList()
        override suspend fun upsertAll(items: List<UserEntity>) {
            rows = items.toMutableList()
        }
        override suspend fun clear() {
            rows.clear()
        }
    }

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        dao = InMemoryUserDao()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(GitHubApi::class.java)
        repository = DefaultUserRepository(api, dao, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun downloadsUsersAndCachesThem() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"id":1,"login":"octocat","avatar_url":"https://example.com/a.png"}]"""
            )
        )

        val users = repository.users(forceRefresh = true)

        assertEquals("octocat", users.single().login)
        assertEquals(1, dao.rows.size)
        assertEquals("/users?per_page=30", server.takeRequest().path)
    }

    @Test
    fun cachedUsersSkipTheNetwork() = runTest {
        dao.rows.add(UserEntity(2, "cached", "https://example.com/b.png"))

        val users = repository.users()

        assertEquals("cached", users.single().login)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun networkFailureFallsBackToCache() = runTest {
        dao.rows.add(UserEntity(3, "offline", "https://example.com/c.png"))
        server.enqueue(MockResponse().setResponseCode(500))

        val users = repository.users(forceRefresh = true)

        assertEquals("offline", users.single().login)
    }

    @Test
    fun networkFailureWithoutCacheIsReported() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = runCatching { repository.users(forceRefresh = true) }

        assertTrue(result.isFailure)
    }
}
