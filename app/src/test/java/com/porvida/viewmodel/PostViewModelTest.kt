package com.porvida.viewmodel

import com.porvida.data.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `postList contiene datos esperados`() = runTest(testDispatcher) {
        val fakePosts = listOf(
            Post(userId = 1, id = 1, title = "Título 1", body = "Contenido 1"),
            Post(userId = 2, id = 2, title = "Título 2", body = "Contenido 2")
        )
        val vm = PostViewModel()
        vm._postList.value = fakePosts
        assertEquals(2, vm.postList.value.size)
        assertEquals("Título 1", vm.postList.value[0].title)
        assertEquals("Contenido 2", vm.postList.value[1].body)
    }

    @Test
    fun `test basico de ejemplo`() = runTest(testDispatcher) {
        assertEquals(1, 1)
    }
}
