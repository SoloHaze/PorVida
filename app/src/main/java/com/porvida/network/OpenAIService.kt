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

object OpenAIService {
    private val json = Json { ignoreUnknownKeys = true }

    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private const val BASE_URL = "https://api.openai.com/v1"

    @Serializable
    data class ResponsesRequest(
        val model: String,
        val input: List<MessageItem>,
        val store: Boolean? = false,
        val tools: List<ToolSpec>? = null,
        val tool_choice: String? = null,
        val include: List<String>? = null
    )

    @Serializable
    data class MessageItem(
        val role: String,
        val content: String
    )

    @Serializable
    data class ToolSpec(
        val type: String,
        val filters: Filters? = null,
        val user_location: UserLocation? = null
    )

    @Serializable
    data class Filters(
        @SerialName("allowed_domains") val allowedDomains: List<String>? = null
    )

    @Serializable
    data class UserLocation(
        val type: String = "approximate",
        val country: String? = null,
        val city: String? = null,
        val region: String? = null,
        val timezone: String? = null
    )

    @Serializable
    data class ResponsesOutputContent(
        val type: String? = null,
        val text: String? = null,
        val annotations: List<AnnotationItem>? = null
    )

    @Serializable
    data class AnnotationItem(
        @SerialName("type") val type: String,
        @SerialName("url") val url: String? = null,
        @SerialName("title") val title: String? = null
    )

    @Serializable
    data class OutputItem(
        val id: String? = null,
        val type: String,
        val status: String? = null,
        val role: String? = null,
        val content: List<ResponsesOutputContent>? = null
    )

    @Serializable
    data class ResponsesResponse(
        val id: String? = null,
        val output: List<OutputItem>? = null
    )

    @Serializable
    data class UrlCitation(
        val url: String,
        val title: String? = null
    )

    data class ChatResult(
        val text: String?,
        val citations: List<UrlCitation> = emptyList()
    )

    fun isConfigured(): Boolean = BuildConfig.OPENAI_API_KEY.isNotBlank()

    /**
     * Send a prompt to OpenAI Responses API. Optionally enable web search tool with domain allow-list.
     * `history` is an alternating list of user/assistant messages.
     */
    fun chat(
        history: List<MessageItem>,
        enableWebSearch: Boolean = false,
        allowedDomains: List<String>? = null,
        model: String = "gpt-5-nano",
        userLocation: UserLocation? = null
    ): Result<ChatResult> {
        if (!isConfigured()) {
            return Result.failure(IllegalStateException("OPENAI_API_KEY missing. Add to local.properties as OPENAI_API_KEY=sk-..."))
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val tools = if (enableWebSearch) {
            listOf(
                ToolSpec(
                    type = "web_search",
                    filters = if (!allowedDomains.isNullOrEmpty()) Filters(allowedDomains) else null,
                    user_location = userLocation
                )
            )
        } else null

        val include = if (enableWebSearch) listOf("web_search_call.action.sources") else null

        val req = ResponsesRequest(
            model = model,
            input = history,
            store = false,
            tools = tools,
            tool_choice = if (enableWebSearch) "auto" else null,
            include = include
        )
        val body = json.encodeToString(ResponsesRequest.serializer(), req).toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL/responses")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .post(body)
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return Result.failure(IllegalStateException("HTTP ${resp.code}"))
                val raw = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString(ResponsesResponse.serializer(), raw)
                val message = parsed.output?.firstOrNull { it.type == "message" }
                val text = message?.content?.firstOrNull { it.type == "output_text" }?.text
                val citations = message?.content?.flatMap { c ->
                    (c.annotations ?: emptyList()).mapNotNull {
                        val url = it.url
                        if (url != null) UrlCitation(url = url, title = it.title) else null
                    }
                } ?: emptyList()
                Result.success(ChatResult(text = text, citations = citations))
            }
        } catch (e: Exception) {
            Log.e("OpenAIService", "chat error", e)
            Result.failure(e)
        }
    }
}
