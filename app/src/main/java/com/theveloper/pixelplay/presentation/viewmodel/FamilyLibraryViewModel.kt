package com.theveloper.pixelplay.presentation.viewmodel

import android.content.Context
import com.theveloper.pixelplay.services.LANDiscoveryService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyLibraryUiState(
    val familySongs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FamilyLibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

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