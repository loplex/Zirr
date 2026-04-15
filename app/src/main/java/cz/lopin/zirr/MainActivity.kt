package cz.lopin.zirr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.lopin.zirr.ui.about.AboutScreen
import cz.lopin.zirr.ui.remote.RemoteScreen
import cz.lopin.zirr.ui.remote.RemoteViewModel
import cz.lopin.zirr.ui.selection.ManufacturerSelectionScreen
import cz.lopin.zirr.ui.selection.ManufacturerViewModel
import cz.lopin.zirr.ui.theme.ZirrTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val appModule = (application as ZirrApplication).appModule

        setContent {
            ZirrTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "check_selection") {
                    composable("check_selection") {
                        val viewModel: RemoteViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return RemoteViewModel(appModule.tvRepository, appModule.irManager) as T
                                }
                            }
                        )
                        val selectedRemote by viewModel.selectedRemote.collectAsState()
                        
                        LaunchedEffect(selectedRemote) {
                            if (selectedRemote == null) {
                                navController.navigate("selection") {
                                    popUpTo("check_selection") { inclusive = true }
                                }
                            } else {
                                navController.navigate("remote") {
                                    popUpTo("check_selection") { inclusive = true }
                                }
                            }
                        }
                    }

                    composable("selection") {
                        val viewModel: ManufacturerViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return ManufacturerViewModel(appModule.tvRepository) as T
                                }
                            }
                        )
                        ManufacturerSelectionScreen(
                            viewModel = viewModel,
                            onBrandSelected = {
                                navController.navigate("remote") {
                                    popUpTo("selection") { inclusive = true }
                                }
                            },
                            onNavigateToAbout = {
                                navController.navigate("about")
                            }
                        )
                    }

                    composable("remote") {
                        val viewModel: RemoteViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    return RemoteViewModel(appModule.tvRepository, appModule.irManager) as T
                                }
                            }
                        )
                        RemoteScreen(
                            viewModel = viewModel,
                            onNavigateToSelection = {
                                navController.navigate("selection")
                            }
                        )
                    }

                    composable("about") {
                        AboutScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToLicenses = { navController.navigate("licenses") }
                        )
                    }

                    @OptIn(ExperimentalMaterial3Api::class)
                    composable("licenses") {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("Open Source Licenses") },
                                    navigationIcon = {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    }
                                )
                            }
                        ) { paddingValues ->
                            val libraries by produceLibraries(R.raw.aboutlibraries)

                            @OptIn(ExperimentalLayoutApi::class)
                            LibrariesContainer(
                                libraries = libraries,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            )
                        }
                    }
                }
            }
        }
    }
}
