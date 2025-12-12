package com.porvida.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.porvida.data.repos.WeatherRepository
import com.porvida.models.WeatherUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WeatherViewModel(app: Application): AndroidViewModel(app) {
    private val repo = WeatherRepository(app.applicationContext)

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val state: StateFlow<WeatherUiState> = _state

    private var lastQuery: String = "auto:ip"

    init { refresh() }

    fun refresh(query: String = lastQuery) {
        lastQuery = query
        _state.value = WeatherUiState.Loading
        viewModelScope.launch {
            val result = repo.fetchWeather(query)
            _state.value = result.fold(
                onSuccess = { WeatherUiState.Success(it) },
                onFailure = { WeatherUiState.Error(it.localizedMessage ?: "Error desconocido") }
            )
        }
    }
}
