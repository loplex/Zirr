package cz.lopin.zirr.ui.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lopin.zirr.data.model.TvBrand
import cz.lopin.zirr.data.repository.TvRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SelectionUiState {
    object Loading : SelectionUiState
    data class Success(
        val brands: List<TvBrand>,
        val filteredBrands: List<TvBrand>,
        val searchQuery: String = ""
    ) : SelectionUiState
    data class Error(val message: String) : SelectionUiState
}

class ManufacturerViewModel(
    private val repository: TvRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SelectionUiState>(SelectionUiState.Loading)
    val uiState: StateFlow<SelectionUiState> = _uiState.asStateFlow()

    init {
        loadBrands()
    }

    fun loadBrands() {
        viewModelScope.launch {
            _uiState.value = SelectionUiState.Loading
            repository.getTvBrands()
                .onSuccess { brands ->
                    _uiState.value = SelectionUiState.Success(
                        brands = brands,
                        filteredBrands = brands
                    )
                }
                .onFailure { error ->
                    _uiState.value = SelectionUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is SelectionUiState.Success) {
            val filtered = if (query.isBlank()) {
                currentState.brands
            } else {
                currentState.brands.filter { it.name.contains(query, ignoreCase = true) }
            }
            _uiState.value = currentState.copy(
                searchQuery = query,
                filteredBrands = filtered
            )
        }
    }

    fun selectBrand(brand: TvBrand, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveRemote(brand.name)
            onSaved()
        }
    }
}
