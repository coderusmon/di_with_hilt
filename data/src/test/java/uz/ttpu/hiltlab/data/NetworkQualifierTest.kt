package uz.ttpu.hiltlab.data

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import uz.ttpu.hiltlab.data.di.NetworkModule
import uz.ttpu.hiltlab.data.remote.FileDownloader

/** Q14: two clients with different timeouts, selected by qualifiers. */
class NetworkQualifierTest {

    @Test
    fun apiClientUsesShortReadTimeout() {
        assertEquals(10_000, NetworkModule.provideApiClient().readTimeoutMillis)
    }

    @Test
    fun downloadClientUsesLongReadTimeout() {
        assertEquals(60_000, NetworkModule.provideDownloadClient().readTimeoutMillis)
    }

    @Test
    fun baseUrlEndsWithSlashForRetrofit() {
        assertEquals("https://api.github.com/", NetworkModule.provideBaseUrl())
    }

    @Test
    fun downloaderReadsResponseBody() {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setBody("file-content"))
            val downloader = FileDownloader(NetworkModule.provideDownloadClient())
            assertEquals("file-content", downloader.download(server.url("/file").toString()))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun downloaderRejectsHttpErrors() {
        val server = MockWebServer().apply { start() }
        try {
            server.enqueue(MockResponse().setResponseCode(404))
            val downloader = FileDownloader(NetworkModule.provideDownloadClient())
            assertThrows(IllegalStateException::class.java) {
                downloader.download(server.url("/missing").toString())
            }
        } finally {
            server.shutdown()
        }
    }
}
