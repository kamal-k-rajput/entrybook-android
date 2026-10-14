package com.greenworldlighting.entrybook.data

import com.greenworldlighting.entrybook.BuildConfig
import com.greenworldlighting.entrybook.core.DEFAULT_BASE_URL
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class ApiException(
    val statusCode: Int,
    val code: String,
    message: String,
    val subscription: Subscription? = null,
) : Exception(message)

class ApiClient(private val store: SessionStore) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    @Volatile private var cachedUrl: String? = null
    @Volatile private var cached: ApiService? = null

    private val bare = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun service(): ApiService {
        val url = store.baseUrl.ifBlank { DEFAULT_BASE_URL }
        val existing = cached
        if (existing != null && cachedUrl == url) return existing
        return synchronized(this) {
            val again = cached
            if (again != null && cachedUrl == url) {
                again
            } else {
                build(url).also {
                    cached = it
                    cachedUrl = url
                }
            }
        }
    }

    fun invalidate() {
        cached = null
        cachedUrl = null
    }

    private fun build(baseUrl: String): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val path = request.url.encodedPath
                val builder = request.newBuilder()
                if (!path.isAuthPath()) {
                    store.accessToken?.let { builder.header("Authorization", "Bearer $it") }
                }
                chain.proceed(builder.build())
            }
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .authenticator { _, response ->
                if (response.request.url.encodedPath.isAuthPath()) return@authenticator null
                if (responseCount(response) >= 2) return@authenticator null
                synchronized(this) {
                    val presented = response.request.header("Authorization")
                        ?.removePrefix("Bearer ")
                        ?.trim()
                    val latest = store.accessToken
                    if (!latest.isNullOrBlank() && latest != presented) {
                        return@synchronized response.request.newBuilder()
                            .header("Authorization", "Bearer $latest")
                            .build()
                    }
                    val refresh = store.refreshToken ?: return@synchronized null
                    val session = refreshSession(store.baseUrl, refresh) ?: return@synchronized null
                    runBlocking { store.save(session) }
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${session.accessToken}")
                        .build()
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }

    private fun refreshSession(baseUrl: String, refreshToken: String): Session? {
        val payload = json.encodeToString(RefreshRequest.serializer(), RefreshRequest(refreshToken))
        val request = Request.Builder()
            .url(baseUrl + "auth/refresh")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            bare.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val text = response.body?.string().orEmpty()
                json.decodeFromString(Envelope.serializer(Session.serializer()), text).data
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

private fun String.isAuthPath(): Boolean =
    contains("/auth/login") || contains("/auth/register") || contains("/auth/refresh") || endsWith("/health")
