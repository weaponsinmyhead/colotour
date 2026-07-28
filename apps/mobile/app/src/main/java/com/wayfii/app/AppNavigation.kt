package com.wayfii.app

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wayfii.app.data.repository.OfflineFirstGamificationRepository
import com.wayfii.app.data.repository.OpenDataItineraryRepository
import com.wayfii.app.data.repository.RemoteGamificationDataSource
import com.wayfii.app.data.repository.RemoteItineraryRepository
import com.wayfii.app.data.repository.ResilientItineraryRepository
import com.wayfii.app.data.repository.SharedPreferencesGamificationStore
import com.wayfii.app.ui.screens.ItineraryScreen
import com.wayfii.app.ui.screens.PreferencesScreen
import com.wayfii.app.ui.viewmodel.ItineraryViewModel

@Composable
fun MainNavigation() {
  val navController = rememberNavController()
  val applicationContext = LocalContext.current.applicationContext

  val itineraryViewModel: ItineraryViewModel = viewModel {
    val localRepository = OpenDataItineraryRepository()
    val apiBaseUrl = BuildConfig.WAYFII_API_BASE_URL.takeIf(String::isNotBlank)
    val remoteRepository = apiBaseUrl?.let(::RemoteItineraryRepository)
    val gamificationRepository = OfflineFirstGamificationRepository(
      localStore = SharedPreferencesGamificationStore(applicationContext),
      remote = apiBaseUrl?.let(::RemoteGamificationDataSource),
    )
    ItineraryViewModel(
      repository = ResilientItineraryRepository(
        primary = remoteRepository,
        localFallback = localRepository,
      ),
      gamificationRepository = gamificationRepository,
    )
  }

  NavHost(
    navController = navController,
    startDestination = PreferencesRoute,
  ) {
    composable<PreferencesRoute> {
      PreferencesScreen(
        onGenerate = { prefs ->
          itineraryViewModel.generarAventuras(prefs)
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
        onSelectProposal = { proposal ->
          itineraryViewModel.seleccionarAventura(proposal)
        },
        onToggleStop = { order ->
          itineraryViewModel.toggleCompletarParada(order)
        },
        onToggleSideQuest = { id ->
          itineraryViewModel.toggleDescubrirSideQuest(id)
        },
        onRewardShown = {
          itineraryViewModel.clearRewardFeedback()
        },
        modifier = Modifier.safeDrawingPadding(),
      )
    }
  }
}
