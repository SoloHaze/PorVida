package com.porvida.remote

import retrofit2.http.GET
import retrofit2.http.Query
import com.porvida.models.WeatherResponse

interface WeatherApiService {
    @GET("forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("days") days: Int = 3,
        @Query("aqi") aqi: String = "yes",
        @Query("alerts") alerts: String = "yes",
        @Query("pollen") pollen: String = "yes"
    ): WeatherResponse
}
