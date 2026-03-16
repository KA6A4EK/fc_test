package com.example.test.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.test.domain.usecase.FetchCidUseCase
import com.example.test.domain.usecase.PingNodeUseCase
import com.example.test.presentation.state.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val fetchCidUseCase: FetchCidUseCase,
    private val pingNodeUseCase: PingNodeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        startPingLoop()
    }

    fun onFetchCid() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, cidError = null) }
            try {
                val result = fetchCidUseCase()
                _uiState.update {
                    it.copy(
                        cidResult = result,
                        loading = false,
                        cidError = null,
                    )
                }
            } catch (e: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        cidError = e.message ?: "Invalid CID",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loading = false,
                        cidError = e.message ?: "Unexpected error while fetching CID",
                    )
                }
            }
        }
    }

    private fun startPingLoop() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val latency = pingNodeUseCase()
                    _uiState.update {
                        it.copy(
                            latency = latency,
                            pingError = null,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(pingError = e.message ?: "Ping failed")
                    }
                }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val PING_INTERVAL_MS: Long = 2_000
    }
}
