package cz.lopin.zirr.ui.remote

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    viewModel: RemoteViewModel,
    onNavigateToSelection: () -> Unit
) {
    val selectedRemote by viewModel.selectedRemote.collectAsState()
    val variants by viewModel.variants.collectAsState()
    val currentIndex by viewModel.currentVariantIndex.collectAsState()

    var showDigits by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = selectedRemote?.brandName ?: "Universal Remote",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (variants.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = viewModel::prevVariant,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "Previous",
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                                Text(
                                    text = "Model ${currentIndex + 1} / ${variants.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = viewModel::nextVariant,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Next",
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    val isFavorite by viewModel.isFavoriteVariant.collectAsState()
                    IconButton(onClick = viewModel::toggleFavoriteVariant) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onNavigateToSelection) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Change Remote")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    Spacer(Modifier.weight(1f))
                    FilledTonalIconToggleButton(
                        checked = showDigits,
                        onCheckedChange = { showDigits = it }
                    ) {
                        Icon(
                            if (showDigits) Icons.Rounded.Apps else Icons.Rounded.Dialpad,
                            contentDescription = "Toggle Keypad"
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showDigits) {
                NumericKeypad(onDigitClick = viewModel::onDigitClick)
            } else {
                MainRemoteControls(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainRemoteControls(viewModel: RemoteViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Top row: Power and Mute
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteButton(
                icon = Icons.Rounded.PowerSettingsNew,
                label = "Power",
                onClick = viewModel::onPowerClick,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
            RemoteButton(
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                label = "Mute",
                onClick = viewModel::onMute
            )
        }

        // Navigation Pad
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ) {}

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = viewModel::onNavUp, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Up", modifier = Modifier.size(48.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = viewModel::onNavLeft, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Left", modifier = Modifier.size(48.dp))
                    }
                    Button(
                        onClick = viewModel::onNavOk,
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = viewModel::onNavRight, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Right", modifier = Modifier.size(48.dp))
                    }
                }
                IconButton(onClick = viewModel::onNavDown, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Down", modifier = Modifier.size(48.dp))
                }
            }
        }

        // Middle row: Menu and Home
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteButton(
                icon = Icons.Rounded.Menu,
                label = "Menu",
                onClick = viewModel::onMenu
            )
            RemoteButton(
                icon = Icons.Rounded.Home,
                label = "Home",
                onClick = { /* Home action */ }
            )
        }

        // Bottom section: Volume
        Card(
            modifier = Modifier.wrapContentSize(),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                IconButton(onClick = viewModel::onVolumeDown) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Volume Down")
                }
                Text(
                    "VOL",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = viewModel::onVolumeUp) {
                    Icon(Icons.Rounded.Add, contentDescription = "Volume Up")
                }
            }
        }
    }
}

@Composable
fun NumericKeypad(onDigitClick: (Int) -> Unit) {
    val digits = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, -1, 0, -2)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(digits) { digit ->
            if (digit >= 0) {
                LargeNumericButton(digit = digit) { onDigitClick(digit) }
            } else {
                Spacer(modifier = Modifier.size(80.dp))
            }
        }
    }
}

@Composable
fun LargeNumericButton(digit: Int, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RemoteButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LargeFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = containerColor,
            contentColor = contentColor,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
