package cz.lopin.zirr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.lopin.zirr.ui.remote.RemoteScreen
import cz.lopin.zirr.ui.remote.RemoteViewModel
import cz.lopin.zirr.ui.selection.ManufacturerSelectionScreen
import cz.lopin.zirr.ui.selection.ManufacturerViewModel
import cz.lopin.zirr.ui.theme.ZirrTheme

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
                }
            }
        }
    }
}
