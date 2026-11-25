package uz.ttpu.hiltlab.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request
import uz.ttpu.hiltlab.data.di.DownloadClient

/** Uses the 60-second client, not the 10-second API client. */
@Singleton
class FileDownloader @Inject constructor(
    @DownloadClient private val client: OkHttpClient,
) {
    fun download(url: String): String {
        val request = Request.Builder().url(url).build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            response.body?.string().orEmpty()
        }
    }
}
