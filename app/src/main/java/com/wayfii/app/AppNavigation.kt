package com.wayfii.app

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wayfii.app.data.repository.JournalRepository
import com.wayfii.app.data.repository.MockItineraryRepository
import com.wayfii.app.ui.screens.*
import com.wayfii.app.ui.viewmodel.ItineraryViewModel

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    val itineraryViewModel: ItineraryViewModel = viewModel {
        ItineraryViewModel(MockItineraryRepository())
    }

    val journalRepository = remember { JournalRepository() }

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
                onOpenJournal = {
                    navController.navigate(JournalRoute)
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
                onBackToProposals = {
                    itineraryViewModel.volverAPropuestas()
                },
                onStartAdventure = { proposal ->
                    itineraryViewModel.iniciarAventura(proposal)
                },
                onToggleStop = { order ->
                    itineraryViewModel.toggleCompletarParada(order)
                },
                onToggleSideQuest = { id ->
                    itineraryViewModel.toggleDescubrirSideQuest(id)
                },
                onFinishAdventure = {
                    itineraryViewModel.finalizarAventura()
                },
                onSaveToJournal = { proposal ->
                    journalRepository.addCompletedAdventure(proposal)
                    navController.navigate(JournalRoute)
                },
                onOpenJournal = {
                    navController.navigate(JournalRoute)
                },
                modifier = Modifier.safeDrawingPadding(),
            )
        }
        composable<JournalRoute> {
            val entries by journalRepository.entries.collectAsStateWithLifecycle()
            val collections by journalRepository.collections.collectAsStateWithLifecycle()
            val memories by journalRepository.memories.collectAsStateWithLifecycle()

            JournalHomeScreen(
                entries = entries,
                collections = collections,
                memories = memories,
                onSelectEntry = { entry ->
                    navController.navigate(JournalDetailRoute(entry.id))
                },
                onOpenStamps = {
                    navController.navigate(PassportStampsRoute)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<JournalDetailRoute> { backStackEntry ->
            val route: JournalDetailRoute = backStackEntry.destination.let {
                // Compose Type Safe Navigation argument retrieval
                val entryId = backStackEntry.arguments?.getString("entryId") ?: ""
                JournalDetailRoute(entryId)
            }

            val entries by journalRepository.entries.collectAsStateWithLifecycle()
            val entry = entries.find { it.id == route.entryId } ?: entries.first()

            JournalAdventureDetailScreen(
                entry = entry,
                onBack = { navController.popBackStack() }
            )
        }
        composable<PassportStampsRoute> {
            val entries by journalRepository.entries.collectAsStateWithLifecycle()
            val stamps = entries.map { it.passportStamp }

            PassportStampsScreen(
                stamps = stamps,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
