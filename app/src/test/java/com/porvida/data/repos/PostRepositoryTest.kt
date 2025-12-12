package com.porvida.data.repos

import com.porvida.data.model.Post
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class PostRepositoryTest : StringSpec({
    "getPosts() retorna lista simulada" {
        val fakePosts = listOf(
            Post(userId = 1, id = 1, title = "Título 1", body = "Cuerpo 1"),
            Post(userId = 2, id = 2, title = "Título 2", body = "Cuerpo 2")
        )
        val repo = mockk<PostRepository>()
        coEvery { repo.getPosts() } returns fakePosts
        runTest {
            val result = repo.getPosts()
            result shouldContainExactly fakePosts
        }
    }
})
