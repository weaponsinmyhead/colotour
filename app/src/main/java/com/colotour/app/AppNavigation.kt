package com.colotour.app

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.colotour.app.data.repository.MockItineraryRepository
import com.colotour.app.ui.screens.ItineraryScreen
import com.colotour.app.ui.screens.PreferencesScreen
import com.colotour.app.ui.viewmodel.ItineraryViewModel

@Composable
fun MainNavigation() {
  val navController = rememberNavController()

  // ViewModel compartido a nivel de navegación
  val itineraryViewModel: ItineraryViewModel = viewModel {
    ItineraryViewModel(MockItineraryRepository())
  }

  NavHost(
    navController = navController,
    startDestination = PreferencesRoute,
  ) {
    composable<PreferencesRoute> {
      PreferencesScreen(
        onGenerate = { prefs ->
          itineraryViewModel.generarItinerario(prefs)
          navController.navigate(ItineraryRoute)
        },
        modifier = Modifier.safeDrawingPadding().padding(8.dp),
      )
    }
    composable<ItineraryRoute> {
      val uiState by itineraryViewModel.uiState.collectAsStateWithLifecycle()

      ItineraryScreen(
        uiState = uiState,
        onBack = {
          itineraryViewModel.resetState()
          navController.popBackStack()
        },
        modifier = Modifier.safeDrawingPadding(),
      )
    }
  }
}
