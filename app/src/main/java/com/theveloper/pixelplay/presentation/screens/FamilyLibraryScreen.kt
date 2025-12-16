package com.theveloper.pixelplay.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.presentation.viewmodel.FamilyLibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyLibraryScreen(
    onSongClick: (Song) -> Unit,
    onBackClick: () -> Unit
) {
    val viewModel: FamilyLibraryViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Family Library") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.fetchFromFamily() }) {
                Icon(Icons.Default.Sync, contentDescription = "Fetch from Family")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.familySongs) { song ->
                FamilySongItem(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
fun FamilySongItem(song: Song, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.title, style = MaterialTheme.typography.titleMedium)
                Text(text = song.artist, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                text = if (song.isFavorite) "Offline" else "Online",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}