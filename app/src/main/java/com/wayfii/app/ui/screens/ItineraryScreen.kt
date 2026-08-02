package com.wayfii.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfii.app.data.model.*
import com.wayfii.app.domain.engine.director.AdventureDirector
import com.wayfii.app.ui.components.*
import com.wayfii.app.ui.viewmodel.ItineraryUiState

private val ScreenBg = Color(0xFFF8F9FA)
private val CardBg = Color.White
private val BorderCol = Color(0xFFE2E8F0)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val TealAccent = Color(0xFF00897B)
private val CoralAccent = Color(0xFFFF5A5F)

@Composable
fun ItineraryScreen(
    uiState: ItineraryUiState,
    onBack: () -> Unit,
    onSelectProposal: (AdventureProposal) -> Unit = {},
    onBackToProposals: () -> Unit = {},
    onStartAdventure: (AdventureProposal) -> Unit = {},
    onToggleStop: (Int) -> Unit = {},
    onToggleSideQuest: (String) -> Unit = {},
    onFinishAdventure: () -> Unit = {},
    onSaveToJournal: (AdventureProposal) -> Unit = {},
    onOpenJournal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        when (uiState) {
            is ItineraryUiState.Idle -> {
                EmptyState(
                    icon = Icons.Default.LocationOn,
                    title = "Descubrí tu aventura",
                    subtitle = "Ingresá tu ubicación y tiempo disponible para encontrar aventuras"
                )
            }

            is ItineraryUiState.Loading -> {
                LoadingState(
                    title = "Diseñando aventuras...",
                    subtitle = "Buscando gemas ocultas y experiencias únicas"
                )
            }

            is ItineraryUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = onBack
                )
            }

            is ItineraryUiState.ProposalsLoaded -> {
                AdventureProposalsScreen(
                    proposals = uiState.proposals,
                    preferences = uiState.preferences,
                    onSelectProposal = onSelectProposal,
                    onBack = onBack
                )
            }

            is ItineraryUiState.ProposalDetail -> {
                AdventureDetailScreen(
                    proposal = uiState.selectedProposal,
                    onBack = onBackToProposals,
                    onStartAdventure = { onStartAdventure(uiState.selectedProposal) }
                )
            }

            is ItineraryUiState.AdventureActive -> {
                ActiveAdventureQuestView(
                    proposal = uiState.selectedProposal,
                    completedStopOrders = uiState.completedStopOrders,
                    discoveredSideQuestIds = uiState.discoveredSideQuestIds,
                    isFinished = uiState.isFinished,
                    onToggleStop = onToggleStop,
                    onToggleSideQuest = onToggleSideQuest,
                    onFinishAdventure = onFinishAdventure,
                    onSaveToJournal = { onSaveToJournal(uiState.selectedProposal) },
                    onBack = onBackToProposals
                )
            }
        }
    }
}

@Composable
fun ActiveAdventureQuestView(
    proposal: AdventureProposal,
    completedStopOrders: Set<Int>,
    discoveredSideQuestIds: Set<String>,
    isFinished: Boolean,
    onToggleStop: (Int) -> Unit,
    onToggleSideQuest: (String) -> Unit,
    onFinishAdventure: () -> Unit,
    onSaveToJournal: () -> Unit = {},
    onBack: () -> Unit
) {
    val director = remember { AdventureDirector() }

    // Simulation states for live Adventure Director
    var isSimulatingRain by remember { mutableStateOf(false) }
    var isSimulatingSunset by remember { mutableStateOf(false) }
    var isSimulatingSurprise by remember { mutableStateOf(false) }
    var activeDirectorSurprise by remember { mutableStateOf<DirectorSurpriseMoment?>(null) }

    var selectedStopOrder by remember { mutableStateOf<Int?>(proposal.mainQuestStops.firstOrNull()?.order) }
    var activePostcard by remember { mutableStateOf<TravelPostcard?>(null) }
    var activeSideQuestBanner by remember { mutableStateOf<SideQuestItem?>(proposal.sideQuests.firstOrNull()) }

    val carouselState = rememberLazyListState()

    val totalMainStops = proposal.mainQuestStops.size
    val completedCount = proposal.mainQuestStops.count { completedStopOrders.contains(it.order) }

    // Evaluate live adaptation from AdventureDirector
    val adaptation = remember(selectedStopOrder, completedCount, isSimulatingRain, isSimulatingSunset, isSimulatingSurprise) {
        val currentOrder = selectedStopOrder ?: 1
        val result = director.evaluateLiveAdaptation(
            proposal = proposal,
            activeChapterOrder = currentOrder,
            elapsedMinutes = completedCount * 20,
            simulatedRain = isSimulatingRain,
            simulatedSunset = isSimulatingSunset,
            simulatedSurprise = isSimulatingSurprise
        )
        if (result.activeSurpriseMoment != null) {
            activeDirectorSurprise = result.activeSurpriseMoment
        }
        result
    }

    val activeChapter = remember(selectedStopOrder, completedStopOrders, adaptation) {
        val stop = proposal.mainQuestStops.find { it.order == selectedStopOrder } ?: proposal.mainQuestStops.firstOrNull()
        stop?.let { s ->
            AdventureChapter(
                chapterNumber = s.order,
                chapterTitle = "Capítulo ${s.order}: ${s.titulo.replace("Inicio: ", "")}",
                narrative = adaptation.liveNarrative,
                discoveryText = s.funFact.ifBlank { s.historicalInfo.ifBlank { "Un lugar especial en la historia de la ciudad." } },
                stopItem = s,
                isUnlocked = true,
                isCompleted = completedStopOrders.contains(s.order)
            )
        }
    }

    LaunchedEffect(selectedStopOrder) {
        selectedStopOrder?.let { order ->
            val index = proposal.mainQuestStops.indexOfFirst { it.order == order }
            if (index != -1) {
                carouselState.animateScrollToItem(index)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Progressive Map Layer (Completed = Solid Teal, Active Leg = Vibrant Coral, Future = Dashed)
        ItineraryMapView(
            stops = proposal.mainQuestStops,
            selectedStopOrder = selectedStopOrder,
            onMarkerClick = { order -> selectedStopOrder = order },
            completedStopOrders = completedStopOrders,
            sideQuests = proposal.sideQuests,
            onSideQuestClick = { sq ->
                activeSideQuestBanner = sq
                onToggleSideQuest(sq.id)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. ADVENTURE DIRECTOR LIVE SIMULATION CONTROL BAR
        DirectorControlBar(
            isSimulatingRain = isSimulatingRain,
            isSimulatingSunset = isSimulatingSunset,
            isSimulatingSurprise = isSimulatingSurprise,
            onToggleRain = {
                isSimulatingRain = !isSimulatingRain
                if (isSimulatingRain) isSimulatingSunset = false
            },
            onToggleSunset = {
                isSimulatingSunset = !isSimulatingSunset
                if (isSimulatingSunset) isSimulatingRain = false
            },
            onToggleSurprise = {
                isSimulatingSurprise = !isSimulatingSurprise
            },
            onReset = {
                isSimulatingRain = false
                isSimulatingSunset = false
                isSimulatingSurprise = false
                activeDirectorSurprise = null
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // 3. Floating Quest HUD (Story & Objective Companion with Dynamic Adaptation Narrative)
        FloatingQuestHud(
            proposal = proposal,
            activeChapter = activeChapter,
            completedCount = completedCount,
            totalChapters = totalMainStops,
            onBack = onBack,
            onPreviousChapter = {
                selectedStopOrder?.let { order ->
                    if (order > 1) selectedStopOrder = order - 1
                }
            },
            onNextChapter = {
                selectedStopOrder?.let { order ->
                    if (order < totalMainStops) selectedStopOrder = order + 1
                }
            },
            onCompleteChapter = { order ->
                onToggleStop(order)
                val completedStop = proposal.mainQuestStops.find { it.order == order }
                completedStop?.let { s ->
                    activePostcard = TravelPostcard(
                        id = "postcard_$order",
                        chapterTitle = "Capítulo $order Descubierto",
                        locationName = s.titulo.replace("Inicio: ", ""),
                        heroImageUrl = s.imageUrl ?: proposal.heroImageUrl ?: "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1000&auto=format&fit=crop",
                        shortStory = s.descripcion,
                        funFact = s.funFact.ifBlank { s.historicalInfo.ifBlank { "Construcción conservada desde 1900 en la ciudad." } }
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 65.dp, start = 16.dp, end = 16.dp)
        )

        // 4. DIRECTOR SURPRISE MOMENT FLOATING BANNER
        activeDirectorSurprise?.let { surprise ->
            DirectorSurpriseBanner(
                surpriseMoment = surprise,
                onAction = { activeDirectorSurprise = null },
                onDismiss = { activeDirectorSurprise = null },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 280.dp, start = 16.dp, end = 16.dp)
            )
        }

        // 5. Floating Proximity Side Quest Notification Banner
        if (activeDirectorSurprise == null) {
            activeSideQuestBanner?.let { sq ->
                ProximitySideQuestBanner(
                    sideQuest = sq,
                    onExplore = { item ->
                        onToggleSideQuest(item.id)
                        activeSideQuestBanner = null
                    },
                    onDismiss = { activeSideQuestBanner = null },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 280.dp, start = 16.dp, end = 16.dp)
                )
            }
        }

        // 6. Quest Chapters Bottom Carousel
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            LazyRow(
                state = carouselState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(proposal.mainQuestStops) { stop ->
                    val isCompleted = completedStopOrders.contains(stop.order)
                    QuestStopCard(
                        stop = stop,
                        isSelected = stop.order == selectedStopOrder,
                        isCompleted = isCompleted,
                        onClick = { selectedStopOrder = stop.order },
                        onToggleComplete = {
                            onToggleStop(stop.order)
                            activePostcard = TravelPostcard(
                                id = "postcard_${stop.order}",
                                chapterTitle = "Capítulo ${stop.order} Descubierto",
                                locationName = stop.titulo.replace("Inicio: ", ""),
                                heroImageUrl = stop.imageUrl ?: proposal.heroImageUrl ?: "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1000&auto=format&fit=crop",
                                shortStory = stop.descripcion,
                                funFact = stop.funFact.ifBlank { stop.historicalInfo.ifBlank { "Un sitio emblemático con encanto histórico." } }
                            )
                        },
                        modifier = Modifier.width(310.dp)
                    )
                }
            }
        }

        // 7. DISCOVERY MOMENT: Collectible Travel Postcard Overlay Modal
        activePostcard?.let { postcard ->
            TravelPostcardOverlay(
                postcard = postcard,
                onContinue = {
                    activePostcard = null
                    selectedStopOrder?.let { currentOrder ->
                        if (currentOrder < totalMainStops) {
                            selectedStopOrder = currentOrder + 1
                        }
                    }
                }
            )
        }

        // 8. Celebration Dialog Overlay (All Chapters Finished)
        if (isFinished && activePostcard == null) {
            CelebrationOverlay(
                proposal = proposal,
                completedCount = completedCount,
                discoveredSideQuestsCount = discoveredSideQuestIds.size,
                onDismiss = onSaveToJournal
            )
        }
    }
}

@Composable
private fun QuestStopCard(
    stop: ItineraryStop,
    isSelected: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Capítulo ${stop.order}: ${stop.titulo}" },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF0FDF4) else CardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isCompleted) Color(0xFF22C55E) else if (isSelected) TealAccent else BorderCol
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isCompleted) Color(0xFF22C55E) else TealAccent.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isCompleted) "✓" else "${stop.order}",
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Color.White else TealAccent,
                            fontSize = 14.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Capítulo ${stop.order} · ${stop.horaInicio}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stop.titulo.replace("Inicio: ", ""),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = stop.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Complete button
            Button(
                onClick = onToggleComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0xFF22C55E) else TealAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (isCompleted) "✓ Capítulo Completado" else "Llegué a este capítulo ✦",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CelebrationOverlay(
    proposal: AdventureProposal,
    completedCount: Int,
    discoveredSideQuestsCount: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "🎉", fontSize = 48.sp)
                Text(
                    text = "¡Aventura Completada!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = TextDark
                )
                Text(
                    text = "Completaste todos los capítulos de ${proposal.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                HorizontalDivider(color = BorderCol)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CelebrationStatItem(label = "Distancia", value = proposal.distanceText)
                    CelebrationStatItem(label = "Capítulos", value = "$completedCount/${proposal.mainQuestStops.size}")
                    CelebrationStatItem(label = "Side Quests", value = "$discoveredSideQuestsCount")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralAccent, contentColor = Color.White)
                ) {
                    Text("¡Excelente! Guardar en mi Diario", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun CelebrationStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = TealAccent)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
