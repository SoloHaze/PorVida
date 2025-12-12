package com.porvida.remote

import com.porvida.data.model.Post
import retrofit2.http.GET

interface ApiService {
	@GET("posts")
	suspend fun getPosts(): List<Post>
}
