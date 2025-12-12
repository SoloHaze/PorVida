package com.porvida.network

import android.util.Log
import com.porvida.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object GeminiService {
    private val json = Json { ignoreUnknownKeys = true }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private const val MODEL = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    @Serializable
    private data class GenerateContentRequest(
        @SerialName("contents") val contents: List<Content>
    )

    @Serializable
    private data class Content(@SerialName("parts") val parts: List<Part>)

    @Serializable
    private data class Part(@SerialName("text") val text: String)

    @Serializable
    private data class GenerateContentResponse(
        @SerialName("candidates") val candidates: List<Candidate>? = null
    )

    @Serializable
    private data class Candidate(@SerialName("content") val content: Content? = null)

    fun isConfigured(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()

    fun chatSimple(prompt: String): Result<String> {
        if (!isConfigured()) return Result.failure(IllegalStateException("GEMINI_API_KEY missing"))
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val bodyObj = GenerateContentRequest(contents = listOf(Content(parts = listOf(Part(text = prompt)))))
        val body = json.encodeToString(GenerateContentRequest.serializer(), bodyObj).toRequestBody(mediaType)
        val url = "$BASE_URL/models/$MODEL:generateContent?key=${BuildConfig.GEMINI_API_KEY}"
        val request = Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return Result.failure(IllegalStateException("HTTP ${resp.code}"))
                val raw = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString(GenerateContentResponse.serializer(), raw)
                val text = parsed.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text.isNullOrBlank()) Result.failure(IllegalStateException("Empty response")) else Result.success(text)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "chatSimple error", e)
            Result.failure(e)
        }
    }

    // Java-friendly wrapper
    data class ChatResponse(val text: String?, val error: String?)

    @JvmStatic
    fun chatSimpleJava(prompt: String): ChatResponse {
        val result = chatSimple(prompt)
        return if (result.isSuccess) {
            ChatResponse(result.getOrNull(), null)
        } else {
            ChatResponse(null, result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }
}
