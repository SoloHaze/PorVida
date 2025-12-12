package com.porvida.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.porvida.data.model.Post
import com.porvida.data.repos.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {
    // Exposed for tests (made public)
    val repository = PostRepository()
    // Internal so test code in same module can set it
    internal val _postList = MutableStateFlow<List<Post>>(emptyList())
    val postList: StateFlow<List<Post>> = _postList

 // Se llama automaticamente al inicio

 init{
     fetchPosts()
 }



// Manejo del flujo en segundo plano

    private fun fetchPosts() {
        viewModelScope.launch {
            try{
                _postList.value = repository.getPosts()
            } catch(e:Exception){
                println("Error al obtener datos: ${e.localizedMessage}")
            }
        }// fin launch

    }   // fin fun
}