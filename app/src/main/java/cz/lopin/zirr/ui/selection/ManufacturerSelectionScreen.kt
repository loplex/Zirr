package cz.lopin.zirr.ui.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import cz.lopin.zirr.data.model.TvBrand
import cz.lopin.zirr.data.local.RemoteEntity

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManufacturerSelectionScreen(
    viewModel: ManufacturerViewModel,
    onBrandSelected: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState is SelectionUiState.Search) "Search TV Brand" else "Favorite Remotes")
                },
                navigationIcon = {
                    if (uiState is SelectionUiState.Search) {
                        IconButton(onClick = viewModel::showFavorites) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Favorites")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is SelectionUiState.Favorites) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::showSearch,
                    icon = { Icon(Icons.Default.Search, contentDescription = "Add Remote") },
                    text = { Text("Search") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SelectionUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SelectionUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = {
                            if (uiState is SelectionUiState.Search) viewModel.showSearch() else viewModel.loadFavorites()
                        }) {
                            Text("Retry")
                        }
                    }
                }
                is SelectionUiState.Favorites -> {
                    if (state.favorites.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No favorite remotes found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val groupedFavorites = state.favorites.groupBy { it.brandName }.toList()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = groupedFavorites.size,
                                key = { index -> groupedFavorites[index].first }
                            ) { index ->
                                val (brandName, remotes) = groupedFavorites[index]
                                BrandFavoritesRow(
                                    brandName = brandName,
                                    remotes = remotes,
                                    onRemoteClick = { remote ->
                                        viewModel.selectFavorite(remote, onBrandSelected)
                                    },
                                    onUnfavoriteClick = { remote ->
                                        viewModel.removeFavorite(remote)
                                    }
                                )
                            }
                        }
                    }
                }
                is SelectionUiState.Search -> {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Search brands...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = state.filteredBrands.size
                        ) { index ->
                            val brand = state.filteredBrands[index]
                            BrandItem(brand = brand) {
                                viewModel.selectBrand(brand, onBrandSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteItem(remote: RemoteEntity, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(remote.brandName) },
        supportingContent = { remote.modelName?.let { Text(it) } },
        modifier = Modifier.clickable { onClick() }
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun BrandFavoritesRow(
    brandName: String,
    remotes: List<RemoteEntity>,
    onRemoteClick: (RemoteEntity) -> Unit,
    onUnfavoriteClick: (RemoteEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Text(
            text = brandName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            remotes.forEach { remote ->
                key(remote.id) {
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier.combinedClickable(
                                onClick = { onRemoteClick(remote) },
                                onLongClick = { expanded = true }
                            ),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = remote.modelName ?: "Model",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unfavorite") },
                                onClick = {
                                    expanded = false
                                    onUnfavoriteClick(remote)
                                }
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
fun BrandItem(brand: TvBrand, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(brand.name) },
        modifier = Modifier.clickable { onClick() }
    )
}
