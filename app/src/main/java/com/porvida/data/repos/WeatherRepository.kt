package com.porvida.data.repos

import android.content.Context
import android.content.pm.PackageManager
import com.google.gson.GsonBuilder
import com.porvida.BuildConfig
import com.porvida.models.WeatherResponse
import com.porvida.remote.WeatherApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository(private val context: Context) {

    private val service: WeatherApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val gson = GsonBuilder().create()
        Retrofit.Builder()
            .baseUrl("https://api.weatherapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()
            .create(WeatherApiService::class.java)
    }

    private fun readApiKeyFromManifest(): String? {
        return try {
            val ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            ai.metaData?.getString("WEATHER_API_KEY")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchWeather(query: String, days: Int = 3): Result<WeatherResponse> {
        val key = readApiKeyFromManifest() ?: BuildConfig.WEATHER_API_KEY
        if (key.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Weather API key not set. Add manifestPlaceholders[WEATHER_API_KEY]."))
        }
        return try {
            val response = service.getForecast(key, query, days = days)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
