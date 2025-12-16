package com.theveloper.pixelplay.presentation.viewmodel

import com.theveloper.pixelplay.data.model.Song
import androidx.lifecycle.ViewModel
import com.theveloper.pixelplay.services.LANDiscoveryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FamilyLibraryUiState(
    val familySongs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

class FamilyLibraryViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyLibraryUiState())
    val uiState: StateFlow<FamilyLibraryUiState> = _uiState.asStateFlow()

    init {
        loadFamilySongs()
    }

    private fun loadFamilySongs() {
        // Load songs from all devices, mark offline/online
        // Placeholder
        _uiState.value = FamilyLibraryUiState(
            familySongs = emptyList() // TODO: Implement
        )
    }

    fun fetchFromFamily() {
        // Start LAN sync
        val intent = android.content.Intent(context, LANDiscoveryService::class.java)
        context.startService(intent)
    }
}