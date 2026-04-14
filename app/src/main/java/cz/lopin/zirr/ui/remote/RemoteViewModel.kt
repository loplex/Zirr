package cz.lopin.zirr.ui.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.lopin.zirr.data.local.RemoteEntity
import cz.lopin.zirr.data.model.IrCommand
import cz.lopin.zirr.data.model.RemoteVariant
import cz.lopin.zirr.data.repository.TvRepository
import cz.lopin.zirr.service.IrManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemoteViewModel(
    private val repository: TvRepository,
    private val irManager: IrManager
) : ViewModel() {

    private val _errorEvents = MutableSharedFlow<String>()
    val errorEvents: SharedFlow<String> = _errorEvents

    val selectedRemote: StateFlow<RemoteEntity?> = repository.getSelectedRemote()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _variants = MutableStateFlow<List<RemoteVariant>>(emptyList())
    val variants: StateFlow<List<RemoteVariant>> = _variants

    private val _currentVariantIndex = MutableStateFlow(0)
    val currentVariantIndex: StateFlow<Int> = _currentVariantIndex

    val currentVariant: StateFlow<RemoteVariant?> = combine(_variants, _currentVariantIndex) { list, idx ->
        list.getOrNull(idx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isFavoriteVariant: StateFlow<Boolean> = combine(
        repository.getFavoriteRemotes(),
        selectedRemote,
        _currentVariantIndex
    ) { favorites, selected, idx ->
        if (selected == null) return@combine false
        val modelStr = "Model ${idx + 1}"
        favorites.any { it.brandName == selected.brandName && it.modelName == modelStr }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            var currentBrand: String? = null

            selectedRemote.collectLatest { remote ->
                if (remote != null) {
                    if (currentBrand != remote.brandName) {
                        currentBrand = remote.brandName
                        val loadedVariants = repository.getRemoteVariants(remote.brandName)
                        _variants.value = loadedVariants

                        // Try to find the index of the previously saved model, otherwise 0
                        val valIndex = loadedVariants.indexOfFirst { "Model ${loadedVariants.indexOf(it) + 1}" == remote.modelName }.coerceAtLeast(0)
                        _currentVariantIndex.value = valIndex
                    }
                } else {
                    currentBrand = null
                    _variants.value = emptyList()
                    _currentVariantIndex.value = 0
                }
            }
        }
    }

    fun nextVariant() {
        val currentSize = _variants.value.size
        if (currentSize > 0) {
            val nextIdx = (_currentVariantIndex.value + 1) % currentSize
            updateVariant(nextIdx)
        }
    }

    fun prevVariant() {
        val currentSize = _variants.value.size
        if (currentSize > 0) {
            val prevIdx = if (_currentVariantIndex.value > 0) _currentVariantIndex.value - 1 else currentSize - 1
            updateVariant(prevIdx)
        }
    }

    private fun updateVariant(index: Int) {
        _currentVariantIndex.value = index
        val remote = selectedRemote.value
        if (remote != null) {
            viewModelScope.launch {
                repository.updateRemoteModel(remote.id, "Model ${index + 1}")
            }
        }
    }

    fun toggleFavoriteVariant() {
        val remote = selectedRemote.value ?: return
        val currentBrand = remote.brandName
        val modelStr = "Model ${_currentVariantIndex.value + 1}"
        val isFav = isFavoriteVariant.value

        viewModelScope.launch {
            if (isFav) {
                repository.removeFavorite(currentBrand, modelStr)
            } else {
                repository.addFavorite(currentBrand, modelStr)
            }
        }
    }

    private fun transmitKey(key: String) {
        val variant = currentVariant.value
        if (variant == null || variant.keys == null) {
            viewModelScope.launch { _errorEvents.emit("No remote control is loaded") }
            return
        }

        val patterns = variant.keys[key]
        if (!patterns.isNullOrEmpty()) {
            val pattern = patterns.first()  // TODO: So far we only support one pattern per key
            transmit(IrCommand(38000, pattern))
        } else {
            viewModelScope.launch {
                _errorEvents.emit("Button '$key' is not available for this model")
            }
        }
    }

    fun onPowerClick() = transmitKey("power")
    fun onVolumeUp() = transmitKey("vol+")
    fun onVolumeDown() = transmitKey("vol-")
    fun onMute() = transmitKey("mute")
    fun onNavUp() = transmitKey("up")
    fun onNavDown() = transmitKey("down")
    fun onNavLeft() = transmitKey("left")
    fun onNavRight() = transmitKey("right")
    fun onNavOk() = transmitKey("ok")
    fun onMenu() = transmitKey("menu")
    fun onDigitClick(digit: Int) = transmitKey(digit.toString())

    private fun transmit(command: IrCommand) {
        if (!irManager.hasIrEmitter()) {
            viewModelScope.launch {
                _errorEvents.emit("Device does not have an IR emitter")
            }
            return
        }
        try {
            irManager.transmit(command)
        } catch (e: Exception) {
            viewModelScope.launch {
                _errorEvents.emit("Transmission error: ${e.message}")
            }
        }
    }
}
