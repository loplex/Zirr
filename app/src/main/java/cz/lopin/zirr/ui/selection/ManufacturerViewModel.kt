package cz.lopin.zirr.ui.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lopin.zirr.data.model.TvBrand
import cz.lopin.zirr.data.repository.TvRepository
import cz.lopin.zirr.data.local.RemoteEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface SelectionUiState {
    object Loading : SelectionUiState
    data class Favorites(
        val favorites: List<RemoteEntity>
    ) : SelectionUiState
    data class Search(
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

    private var latestFavorites: List<RemoteEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.getFavoriteRemotes().collect { remotes ->
                latestFavorites = remotes
                val current = _uiState.value
                if (current !is SelectionUiState.Search) {
                    _uiState.value = SelectionUiState.Favorites(remotes)
                }
            }
        }
    }

    fun loadFavorites() {
        _uiState.value = SelectionUiState.Favorites(latestFavorites)
    }

    fun showSearch() {
        viewModelScope.launch {
            _uiState.value = SelectionUiState.Loading
            repository.getTvBrands()
                .onSuccess { brands ->
                    _uiState.value = SelectionUiState.Search(
                        brands = brands,
                        filteredBrands = brands
                    )
                }
                .onFailure { error ->
                    _uiState.value = SelectionUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun showFavorites() {
        loadFavorites()
    }

    fun onSearchQueryChanged(query: String) {
        val currentState = _uiState.value
        if (currentState is SelectionUiState.Search) {
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
            repository.saveRemote(brand.name, "Model 1")
            onSaved()
        }
    }

    fun selectFavorite(remote: RemoteEntity, onSelected: () -> Unit) {
        viewModelScope.launch {
            repository.saveRemote(remote.brandName, remote.modelName)
            onSelected()
        }
    }

    fun removeFavorite(remote: RemoteEntity) {
        viewModelScope.launch {
            repository.removeFavorite(remote.brandName, remote.modelName ?: "Model 1")
        }
    }
}
