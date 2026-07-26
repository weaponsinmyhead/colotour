package com.example.travelitinerary

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.travelitinerary.data.repository.MockItineraryRepository
import com.example.travelitinerary.ui.screens.ItineraryScreen
import com.example.travelitinerary.ui.screens.PreferencesScreen
import com.example.travelitinerary.ui.viewmodel.ItineraryViewModel

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Preferences)

  // Instanciamos el ViewModel inyectando el Repositorio Mock
  val itineraryViewModel: ItineraryViewModel = viewModel {
    ItineraryViewModel(MockItineraryRepository())
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Preferences> {
          PreferencesScreen(
            onGenerate = { prefs ->
              backStack.add(ItineraryResult(prefs))
            },
            modifier = Modifier.safeDrawingPadding().padding(8.dp)
          )
        }
        entry<ItineraryResult> { key ->
          val prefs = key.preferences
          val uiState by itineraryViewModel.uiState.collectAsStateWithLifecycle()

          // Disparar generación al navegar a esta pantalla
          LaunchedEffect(prefs) {
            itineraryViewModel.generarItinerario(prefs)
          }

          ItineraryScreen(
            uiState = uiState,
            onBack = {
              itineraryViewModel.resetState()
              backStack.removeLastOrNull()
            },
            modifier = Modifier.safeDrawingPadding()
          )
        }
      },
  )
}
