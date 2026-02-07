package com.silverpixelism.hyotok.data

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data Models
data class WeatherResponse(
    @SerializedName("current") val current: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int
)

// Retrofit Interface
interface WeatherApi {
    @GET("v1/forecast?current=temperature_2m,weather_code")
    suspend fun getWeather(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double
    ): WeatherResponse
}

// Repository
object WeatherRepository {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val api: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    suspend fun getCurrentWeather(lat: Double, lon: Double): CurrentWeather? {
        return try {
            val response = api.getWeather(lat, lon)
            response.current
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Helper to get icon and description based on WMO Weather Code
    fun getWeatherInfo(code: Int): Pair<String, String> {
        return when (code) {
            0 -> "맑음" to "☀️"
            1, 2, 3 -> "구름 조금" to "⛅"
            45, 48 -> "안개" to "🌫️"
            51, 53, 55 -> "이슬비" to "🌦️"
            61, 63, 65 -> "비" to "🌧️"
            66, 67 -> "진눈깨비" to "🌨️"
            71, 73, 75 -> "눈" to "❄️"
            77 -> "눈발" to "❄️"
            80, 81, 82 -> "소나기" to "🌦️"
            85, 86 -> "눈 소나기" to "❄️"
            95 -> "천둥번개" to "⚡"
            96, 99 -> "천둥번개 동반 우박" to "⛈️"
            else -> "알 수 없음" to "❓"
        }
    }
}
