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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfii.app.data.model.*
import com.wayfii.app.ui.components.*
import com.wayfii.app.ui.viewmodel.ItineraryUiState
import com.wayfii.app.ui.viewmodel.RewardFeedback

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
    onToggleStop: (Int) -> Unit = {},
    onToggleSideQuest: (String) -> Unit = {},
    onRewardShown: () -> Unit = {},
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

            is ItineraryUiState.AdventureActive -> {
                ActiveAdventureQuestView(
                    proposal = uiState.selectedProposal,
                    completedStopOrders = uiState.completedStopOrders,
                    discoveredSideQuestIds = uiState.discoveredSideQuestIds,
                    playerProgress = uiState.playerProgress,
                    isProgressLoading = uiState.isProgressLoading,
                    isFinished = uiState.isFinished,
                    rewardFeedback = uiState.rewardFeedback,
                    progressError = uiState.progressError,
                    onToggleStop = onToggleStop,
                    onToggleSideQuest = onToggleSideQuest,
                    onRewardShown = onRewardShown,
                    onBack = onBack
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
    playerProgress: PlayerProgress,
    isProgressLoading: Boolean,
    isFinished: Boolean,
    rewardFeedback: RewardFeedback?,
    progressError: String?,
    onToggleStop: (Int) -> Unit,
    onToggleSideQuest: (String) -> Unit,
    onRewardShown: () -> Unit,
    onBack: () -> Unit
) {
    var selectedStopOrder by remember { mutableStateOf<Int?>(proposal.mainQuestStops.firstOrNull()?.order) }
    val carouselState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    val totalMainStops = proposal.mainQuestStops.size
    val completedCount = proposal.mainQuestStops.count { completedStopOrders.contains(it.order) }
    val progressFraction = if (totalMainStops > 0) completedCount.toFloat() / totalMainStops else 0f
    val progressPercent = (progressFraction * 100).toInt()

    LaunchedEffect(selectedStopOrder) {
        selectedStopOrder?.let { order ->
            val index = proposal.mainQuestStops.indexOfFirst { it.order == order }
            if (index != -1) {
                carouselState.animateScrollToItem(index)
            }
        }
    }

    LaunchedEffect(rewardFeedback?.id, isFinished) {
        val feedback = rewardFeedback
        if (feedback != null && !isFinished) {
            snackbarHostState.showSnackbar(feedback.message)
            onRewardShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Map Layer
        ItineraryMapView(
            stops = proposal.mainQuestStops,
            selectedStopOrder = selectedStopOrder,
            onMarkerClick = { order -> selectedStopOrder = order },
            completedStopOrders = completedStopOrders,
            sideQuests = proposal.sideQuests,
            onSideQuestClick = { sq -> onToggleSideQuest(sq.id) },
            modifier = Modifier.fillMaxSize()
        )

        // 2. RPG Header Bar & Progress Indicator
        FloatingQuestHeader(
            proposal = proposal,
            completedCount = completedCount,
            totalMainStops = totalMainStops,
            progressPercent = progressPercent,
            playerProgress = playerProgress,
            isProgressLoading = isProgressLoading,
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        )

        // 3. Floating Side Quests Badge Count
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 170.dp, start = 16.dp),
            shape = RoundedCornerShape(14.dp),
            color = CardBg.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, BorderCol),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("✨ Side Quests:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    text = "${discoveredSideQuestIds.size}/${proposal.sideQuests.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TealAccent
                )
            }
        }

        // 4. Quest Stops Carousel
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
                        onToggleComplete = { onToggleStop(stop.order) },
                        modifier = Modifier.width(310.dp)
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 188.dp),
        )

        // 5. Celebration Dialog Overlay
        if (isFinished) {
            CelebrationOverlay(
                proposal = proposal,
                completedCount = completedCount,
                discoveredSideQuestsCount = discoveredSideQuestIds.size,
                playerProgress = playerProgress,
                rewardFeedback = rewardFeedback,
                progressError = progressError,
                onDismiss = onBack
            )
        }
    }
}

@Composable
private fun FloatingQuestHeader(
    proposal: AdventureProposal,
    completedCount: Int,
    totalMainStops: Int,
    progressPercent: Int,
    playerProgress: PlayerProgress,
    isProgressLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardBg.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, BorderCol)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextDark
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${proposal.emoji} ${proposal.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Main Quest · $completedCount de $totalMainStops completados",
                        style = MaterialTheme.typography.labelSmall,
                        color = TealAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TealAccent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$progressPercent%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = {
                    if (totalMainStops == 0) {
                        0f
                    } else {
                        (completedCount.toFloat() / totalMainStops).coerceIn(0f, 1f)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = TealAccent,
                trackColor = Color(0xFFE2E8F0)
            )

            PlayerProgressSummary(
                progress = playerProgress,
                isLoading = isProgressLoading,
            )
        }
    }
}

@Composable
private fun PlayerProgressSummary(
    progress: PlayerProgress,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProgressMetric(text = "Nivel ${progress.level}")
        ProgressMetric(text = "${progress.points} pts")
        ProgressMetric(text = "🔥 ${progress.currentStreak}")
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (isLoading) "Cargando..." else progress.syncStatus.shortLabel(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = when (progress.syncStatus) {
                GamificationSyncStatus.SYNCED -> TealAccent
                GamificationSyncStatus.PENDING -> CoralAccent
                GamificationSyncStatus.LOCAL_ONLY -> TextMuted
            },
        )
    }
}

@Composable
private fun ProgressMetric(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark,
        )
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
            .semantics { contentDescription = "Main Quest ${stop.order}: ${stop.titulo}" },
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
                        text = stop.horaInicio,
                        style = MaterialTheme.typography.labelSmall,
                        color = TealAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stop.titulo,
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
                enabled = !isCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) Color(0xFF22C55E) else TealAccent,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF22C55E),
                    disabledContentColor = Color.White,
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (isCompleted) "✓ Misión Completada" else "Marcar como completada",
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
    playerProgress: PlayerProgress,
    rewardFeedback: RewardFeedback?,
    progressError: String?,
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
                    text = "Completaste exitosamente ${proposal.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )

                if ((rewardFeedback?.awardedPoints ?: 0) > 0) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CoralAccent.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = "+${rewardFeedback?.awardedPoints} puntos",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = CoralAccent,
                        )
                    }
                }

                rewardFeedback?.earnedBadges?.firstOrNull()?.let { badge ->
                    Text(
                        text = "🏅 Nuevo logro: ${badge.displayName()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                    )
                }

                if (progressError != null) {
                    Text(
                        text = progressError,
                        style = MaterialTheme.typography.bodySmall,
                        color = CoralAccent,
                    )
                } else {
                    Text(
                        text = playerProgress.syncStatus.longLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }

                Text(
                    text = "$completedCount/${proposal.mainQuestStops.size} paradas · " +
                        "$discoveredSideQuestsCount Side Quests · ${proposal.distanceText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )

                HorizontalDivider(color = BorderCol)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CelebrationStatItem(label = "Nivel", value = "${playerProgress.level}")
                    CelebrationStatItem(label = "Puntos", value = "${playerProgress.points}")
                    CelebrationStatItem(label = "Racha", value = "🔥 ${playerProgress.currentStreak}")
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
                    Text("¡Excelente! Volver al inicio", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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

private fun GamificationSyncStatus.shortLabel(): String = when (this) {
    GamificationSyncStatus.SYNCED -> "En línea"
    GamificationSyncStatus.PENDING -> "Pendiente"
    GamificationSyncStatus.LOCAL_ONLY -> "Local"
}

private fun GamificationSyncStatus.longLabel(): String = when (this) {
    GamificationSyncStatus.SYNCED -> "Progreso sincronizado con Wayfii."
    GamificationSyncStatus.PENDING ->
        "Guardado en el teléfono. Se sincronizará cuando vuelva la conexión."
    GamificationSyncStatus.LOCAL_ONLY -> "Progreso guardado en este dispositivo."
}

private fun String.displayName(): String = when (this) {
    "primer_paso" -> "Primer paso"
    "explorador_local" -> "Explorador local"
    "agenda_viva" -> "Agenda viva"
    "curador_comunitario" -> "Curador comunitario"
    "racha_7_dias" -> "Racha de 7 días"
    else -> replace('_', ' ').replaceFirstChar(Char::uppercase)
}
