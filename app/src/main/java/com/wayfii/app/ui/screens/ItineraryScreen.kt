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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wayfii.app.data.model.ActivityVisualType
import com.wayfii.app.data.model.Itinerary
import com.wayfii.app.data.model.ItineraryStop
import com.wayfii.app.data.model.StopType
import com.wayfii.app.ui.components.*
import com.wayfii.app.ui.viewmodel.ItineraryUiState

// ─────────────────────────────────────────────────────────────────────────────
// ItineraryScreen — Rediseño: Mapa como protagonista (Escenario Vivo)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ItineraryScreen(
    uiState: ItineraryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (uiState) {
            is ItineraryUiState.Idle -> {
                EmptyState(
                    icon = Icons.Default.LocationOn,
                    title = "Planifica tu aventura",
                    subtitle = "Define tus preferencias para crear el mapa de tu día"
                )
            }

            is ItineraryUiState.Loading -> {
                LoadingState(
                    title = "Dibujando tu mapa…",
                    subtitle = "Buscando rutas y tesoros locales"
                )
            }

            is ItineraryUiState.Error -> {
                ErrorState(
                    message = uiState.message,
                    onRetry = onBack
                )
            }

            is ItineraryUiState.Success -> {
                ItineraryMapCentricView(
                    itinerary = uiState.itinerary,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
fun ItineraryMapCentricView(
    itinerary: Itinerary,
    onBack: () -> Unit
) {
    var selectedStopOrder by remember { mutableStateOf<Int?>(itinerary.actividades.firstOrNull()?.order) }
    val carouselState = rememberLazyListState()

    // Sincronizar carrusel cuando se selecciona desde el mapa
    LaunchedEffect(selectedStopOrder) {
        selectedStopOrder?.let { order ->
            val index = itinerary.actividades.indexOfFirst { it.order == order }
            if (index != -1) {
                carouselState.animateScrollToItem(index)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. CAPA BASE: EL MAPA (Protagonista)
        val hasCoordinates = itinerary.actividades.any { it.latitud != null && it.longitud != null }
        
        if (hasCoordinates) {
            ItineraryMapView(
                stops = itinerary.actividades,
                selectedStopOrder = selectedStopOrder,
                onMarkerClick = { order -> selectedStopOrder = order },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Fallback visual si no hay mapa
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.background)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Mapa no disponible", style = MaterialTheme.typography.titleLarge)
            }
        }

        // 2. CAPA SUPERIOR: HEADER FLOTANTE
        FloatingHeader(
            destino = itinerary.destino,
            horario = itinerary.rangoHorarioText,
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        )

        // 3. CAPA MEDIA: RESUMEN FLOTANTE (Pills)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 110.dp, start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingPill(
                icon = Icons.Default.Person,
                text = "${itinerary.cantidadPersonas}",
                containerColor = MaterialTheme.colorScheme.surface,
                iconTint = MaterialTheme.colorScheme.secondary
            )
            FloatingPill(
                icon = Icons.Default.Star,
                text = itinerary.costoTotalEstimado.split("(").first().trim(),
                containerColor = MaterialTheme.colorScheme.surface,
                iconTint = MaterialTheme.colorScheme.tertiary
            )
            if (itinerary.incluyeComida) {
                FloatingPill(
                    icon = Icons.Default.Favorite,
                    text = "Gastro",
                    containerColor = MaterialTheme.colorScheme.surface,
                    iconTint = Color(0xFFFF9800)
                )
            }
        }

        // 4. CAPA INFERIOR: CARRUSEL DE PARADAS
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            LazyRow(
                state = carouselState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(itinerary.actividades) { activity ->
                    CarouselCard(
                        activity = activity,
                        isSelected = activity.order == selectedStopOrder,
                        onClick = { selectedStopOrder = activity.order },
                        modifier = Modifier.width(300.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingHeader(
    destino: String,
    horario: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destino,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = horario,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ajustar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CarouselCard(
    activity: ItineraryStop,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = if (isSelected) 1f else 0.95f
    
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.surface 
                             else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 10.dp else 2.dp
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icono/Miniatura con fondo lúdico
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(getVisualTypeBgColor(activity.visualType)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stopTypeIcon(activity.visualType),
                    contentDescription = null,
                    tint = getVisualTypeIconColor(activity.visualType),
                    modifier = Modifier.size(32.dp)
                )
                // Orden en pequeño
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${activity.order}",
                        style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.horaInicio,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activity.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = activity.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers visuales
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun getVisualTypeBgColor(visualType: ActivityVisualType): Color {
    return when (visualType) {
        ActivityVisualType.NATURE -> Color(0xFFE8F5E9)
        ActivityVisualType.ADVENTURE -> Color(0xFFFFF3E0)
        ActivityVisualType.FOOD -> Color(0xFFFFEBEE)
        ActivityVisualType.CULTURE -> Color(0xFFE8EAF6)
        ActivityVisualType.HISTORY -> Color(0xFFEFEBE9)
        ActivityVisualType.PHOTO -> Color(0xFFE0F7FA)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
    }
}

@Composable
private fun getVisualTypeIconColor(visualType: ActivityVisualType): Color {
    return when (visualType) {
        ActivityVisualType.NATURE -> Color(0xFF2E7D32)
        ActivityVisualType.ADVENTURE -> Color(0xFFEF6C00)
        ActivityVisualType.FOOD -> Color(0xFFC62828)
        ActivityVisualType.CULTURE -> Color(0xFF3F51B5)
        ActivityVisualType.HISTORY -> Color(0xFF5D4037)
        ActivityVisualType.PHOTO -> Color(0xFF0097A7)
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun stopTypeIcon(visualType: ActivityVisualType): ImageVector {
    return when (visualType) {
        ActivityVisualType.START      -> Icons.Default.Home
        ActivityVisualType.FOOD       -> Icons.Default.Favorite
        ActivityVisualType.NATURE     -> Icons.Default.FavoriteBorder
        ActivityVisualType.ADVENTURE  -> Icons.Default.LocationOn
        ActivityVisualType.CULTURE    -> Icons.Default.Star
        ActivityVisualType.HISTORY    -> Icons.Default.Info
        ActivityVisualType.SHOPPING   -> Icons.Default.ShoppingCart
        ActivityVisualType.PHOTO      -> Icons.Default.Place
        ActivityVisualType.EVENT      -> Icons.Default.DateRange
        ActivityVisualType.FAMILY     -> Icons.Default.Person
        ActivityVisualType.MAINSTREAM -> Icons.Default.Star
        ActivityVisualType.DEFAULT    -> Icons.Default.LocationOn
    }
}
