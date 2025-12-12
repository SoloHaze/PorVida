package com.porvida.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.porvida.viewmodel.PostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(viewModel: PostViewModel) {
	val posts by viewModel.postList.collectAsState()
	Scaffold(topBar = { TopAppBar(title = { Text("Posts") }) }) { padding ->
		if (posts.isEmpty()) {
			Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
				CircularProgressIndicator()
			}
		} else {
			LazyColumn(
				modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				items(posts) { post ->
					Card { Column(Modifier.padding(12.dp)) {
						Text(post.title ?: "(Sin título)", fontWeight = FontWeight.Bold)
						Spacer(Modifier.height(4.dp))
						Text(post.body ?: "(Sin contenido)")
					}}
				}
			}
		}
	}
}
