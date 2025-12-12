package com.porvida.models

import com.google.gson.annotations.SerializedName

// Core response
data class WeatherResponse(
    val location: Location,
    val current: Current,
    val forecast: Forecast?,
    val alerts: Alerts?
)

// Location object
data class Location(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val tz_id: String,
    val localtime: String
)

// Condition
data class Condition(
    val text: String,
    val icon: String,
    val code: Int
)

// Air Quality
data class AirQuality(
    val co: Float?,
    val o3: Float?,
    val no2: Float?,
    val so2: Float?,
    @SerializedName("pm2_5") val pm2_5: Float?,
    val pm10: Float?,
    @SerializedName("us-epa-index") val usEpaIndex: Int?,
    @SerializedName("gb-defra-index") val gbDefraIndex: Int?
)

// Pollen (may not always present)
data class Pollen(
    val Hazel: Float?,
    val Alder: Float?,
    val Birch: Float?,
    val Oak: Float?,
    val Grass: Float?,
    val Mugwort: Float?,
    val Ragweed: Float?
)

// Current weather
data class Current(
    val last_updated: String?,
    val last_updated_epoch: Long?,
    val temp_c: Float?,
    val temp_f: Float?,
    val feelslike_c: Float?,
    val feelslike_f: Float?,
    val wind_kph: Float?,
    val wind_mph: Float?,
    val wind_degree: Int?,
    val wind_dir: String?,
    val pressure_mb: Float?,
    val pressure_in: Float?,
    val precip_mm: Float?,
    val precip_in: Float?,
    val humidity: Int?,
    val cloud: Int?,
    val is_day: Int?,
    val uv: Float?,
    val gust_kph: Float?,
    val gust_mph: Float?,
    val condition: Condition?,
    @SerializedName("air_quality") val airQuality: AirQuality?,
    @SerializedName("pollen") val pollen: Pollen?
)

// Forecast container
data class Forecast(
    val forecastday: List<ForecastDay>
)

// Forecast day
data class ForecastDay(
    val date: String,
    val date_epoch: Long,
    val day: DayDetail,
    val astro: Astro,
    val hour: List<HourDetail>?
)

// Day detail
data class DayDetail(
    val maxtemp_c: Float?,
    val mintemp_c: Float?,
    val avgtemp_c: Float?,
    val maxwind_kph: Float?,
    val totalprecip_mm: Float?,
    val avghumidity: Int?,
    val daily_chance_of_rain: Int?,
    val daily_chance_of_snow: Int?,
    val condition: Condition?,
    val uv: Float?
)

// Astro
data class Astro(
    val sunrise: String?,
    val sunset: String?,
    val moonrise: String?,
    val moonset: String?,
    val moon_phase: String?,
    val is_moon_up: Int?,
    val is_sun_up: Int?
)

// Hour detail (subset)
data class HourDetail(
    val time: String?,
    val temp_c: Float?,
    val feelslike_c: Float?,
    val wind_kph: Float?,
    val humidity: Int?,
    val will_it_rain: Int?,
    val chance_of_rain: Int?,
    val condition: Condition?
)

// Alerts
data class Alerts(
    val alert: List<Alert>?
)

// Single alert
data class Alert(
    val headline: String?,
    val severity: String?,
    val event: String?,
    val desc: String?,
    val instruction: String?,
    val expires: String?
)

// UI state wrapper
sealed class WeatherUiState {
    object Loading: WeatherUiState()
    data class Success(val data: WeatherResponse): WeatherUiState()
    data class Error(val message: String): WeatherUiState()
}
