package com.porvida.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.porvida.data.model.Post
import com.porvida.viewmodel.PostViewModel
import org.junit.Rule
import org.junit.Test

class PostScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun titulos_de_post_se_muestran_en_pantalla() {
        val fakePosts = listOf(
            Post(userId = 1, id = 1, title = "Título 1", body = "Contenido 1"),
            Post(userId = 2, id = 2, title = "Título 2", body = "Contenido 2")
        )
        val vm = PostViewModel().apply { _postList.value = fakePosts }

        composeRule.setContent { PostScreen(viewModel = vm) }
        composeRule.waitForIdle()

        // Verifica cada título directamente (PostScreen muestra sólo title)
        composeRule.onNodeWithText("Título 1").assertIsDisplayed()
        composeRule.onNodeWithText("Título 2").assertIsDisplayed()
    }
}
