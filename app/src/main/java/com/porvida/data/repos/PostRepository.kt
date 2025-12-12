package com.porvida.data.repos

import com.porvida.data.remote.RetrofitInstance
import com.porvida.data.model.Post

class PostRepository {
	suspend fun getPosts(): List<Post> = RetrofitInstance.api.getPosts()
}
