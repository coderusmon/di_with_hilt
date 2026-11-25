package uz.ttpu.hiltlab.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GitHubApi {
    @GET("users")
    suspend fun users(@Query("per_page") perPage: Int = 30): List<UserDto>
}

data class UserDto(
    val id: Long,
    val login: String,
    @SerializedName("avatar_url") val avatarUrl: String,
)
