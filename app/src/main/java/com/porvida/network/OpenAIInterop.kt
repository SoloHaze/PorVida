package com.porvida.network

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object OpenAIInterop {
    private val json = Json { ignoreUnknownKeys = true }

    @JvmStatic
    fun decodeHistory(jsonString: String): List<OpenAIService.MessageItem> {
        // Note: Java can't use Kotlin reified serializers; expose a concrete one here
        val serializer = ListSerializer(OpenAIService.MessageItem.serializer())
        return json.decodeFromString(serializer, jsonString)
    }

    @JvmStatic
    fun chatBlocking(
        history: List<OpenAIService.MessageItem>,
        enableWebSearch: Boolean,
        allowedDomains: List<String>?,
        model: String,
        country: String?, city: String?, region: String?, timezone: String?
    ): OpenAIService.ChatResult? {
        val loc = if (country != null || city != null || region != null || timezone != null) {
            OpenAIService.UserLocation(
                country = country, city = city, region = region, timezone = timezone
            )
        } else null
        val result = OpenAIService.chat(history, enableWebSearch, allowedDomains, model, loc)
        return result.getOrNull()
    }
}
