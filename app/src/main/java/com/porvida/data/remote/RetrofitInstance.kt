package com.porvida.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.porvida.remote.ApiService

object RetrofitInstance {
	private const val BASE_URL = "https://jsonplaceholder.typicode.com/" // Placeholder API for posts

	private val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
	private val client: OkHttpClient = OkHttpClient.Builder()
		.addInterceptor(logging)
		.build()

	private val retrofit: Retrofit by lazy {
		Retrofit.Builder()
			.baseUrl(BASE_URL)
			.addConverterFactory(GsonConverterFactory.create())
			.client(client)
			.build()
	}

	val api: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
