package com.colotour.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.colotour.app.data.model.ActivityVisualType
import com.colotour.app.data.model.Itinerary
import com.colotour.app.data.model.StopType
import com.colotour.app.ui.viewmodel.ItineraryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    uiState: ItineraryUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu Itinerario Colotour") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                is ItineraryUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Configurá tus preferencias para empezar.")
                    }
                }
                is ItineraryUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Buscando el mejor recorrido...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ajustando horarios y paradas turísticas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                is ItineraryUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ups, algo salió mal",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Volver a intentar")
                        }
                    }
                }
                is ItineraryUiState.Success -> {
                    ItineraryDetails(itinerary = uiState.itinerary, onBack = onBack)
                }
            }
        }
    }
}

@Composable
fun ItineraryDetails(itinerary: Itinerary, onBack: () -> Unit) {
    var selectedStopOrder by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    // Calcular estadísticas dinámicas del viaje
    val totalStops = itinerary.actividades.size
    val touristStops = itinerary.actividades.filter { it.type == StopType.PLACE }
    val visualTypes = touristStops.map { it.visualType }
    val dominantExperience = if (visualTypes.isEmpty()) {
        "Exploración urbana"
    } else {
        val groups = visualTypes.groupBy { it }.maxByOrNull { it.value.size }
        when (groups?.key) {
            ActivityVisualType.NATURE -> "Naturaleza y Paisajes"
            ActivityVisualType.ADVENTURE -> "Aventura y Trekking"
            ActivityVisualType.CULTURE -> "Cultural e Histórica"
            ActivityVisualType.HISTORY -> "Histórica"
            ActivityVisualType.SHOPPING -> "Compras y Ocio"
            ActivityVisualType.PHOTO -> "Fotografía y Vistas"
            ActivityVisualType.EVENT -> "Eventos Locales"
            ActivityVisualType.FAMILY -> "Familiar"
            ActivityVisualType.MAINSTREAM -> "Atracciones Populares"
            else -> "Exploración mixta"
        }
    }

    // Offset dinámico por el header, banner de fallback y resumen estadístico
    val scrollOffset = 4 + (if (itinerary.isFallbackCoordinates) 1 else 0)
    
    LaunchedEffect(selectedStopOrder) {
        selectedStopOrder?.let { order ->
            val index = itinerary.actividades.indexOfFirst { it.order == order }
            if (index != -1) {
                listState.animateScrollToItem(index + scrollOffset)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta Resumen Principal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = itinerary.destino,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (itinerary.dataSourceSummary.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = itinerary.dataSourceSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Partida",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Inicio: ${itinerary.puntoPartida}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Horario",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = itinerary.rangoHorarioText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Personas",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${itinerary.cantidadPersonas} pers.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Comidas",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (itinerary.incluyeComida) "Comidas incluidas" else "Sin comida",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Presupuesto Estimado: ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = itinerary.costoTotalEstimado,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Banner de Fallback con Animación
        item {
            AnimatedVisibility(
                visible = itinerary.isFallbackCoordinates,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Aviso de ubicación",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Ubicación aproximada por falta de geocodificación precisa.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Tarjeta de Resumen Estadístico del Viaje
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resumen del Recorrido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Paradas", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = "$totalStops paradas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "Experiencia Dominante", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = dominantExperience, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "Recorrido", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = "Sugerido", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Mapa OSM integrado
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                val hasCoordinates = itinerary.actividades.any { it.latitud != null && it.longitud != null }
                if (hasCoordinates) {
                    ItineraryMapView(
                        stops = itinerary.actividades,
                        selectedStopOrder = selectedStopOrder,
                        onMarkerClick = { order -> selectedStopOrder = order },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Mapa no disponible",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Mapa no disponible para este itinerario.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Listado Cronológico
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cronograma del Día",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onBack) {
                    Text(text = "Ajustar preferencias", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
        }

        itemsIndexed(itinerary.actividades) { index, activity ->
            val isSelected = activity.order == selectedStopOrder
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Indicador de Línea de Tiempo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(24.dp)
                ) {
                    val bulletColor = when (activity.type) {
                        StopType.START -> MaterialTheme.colorScheme.tertiary
                        StopType.FOOD -> Color(0xFFFF9800)
                        StopType.PLACE -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.tertiary else bulletColor)
                    )
                    if (index < itinerary.actividades.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(140.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        )
                    }
                }

                // Card interactiva de Parada
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedStopOrder = activity.order },
                    shape = RoundedCornerShape(16.dp),
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                ) {
                    Column {
                        // Imagen de cabecera si es tipo PLACE con soporte de Coil y fallback
                        if (activity.type == StopType.PLACE) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                if (!activity.imageUrl.isNullOrBlank()) {
                                    coil.compose.AsyncImage(
                                        model = activity.imageUrl,
                                        contentDescription = activity.titulo,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    val (bgColor, iconColor) = getPlaceholderColors(activity.visualType)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = getIconForVisualType(activity.visualType),
                                            contentDescription = "Placeholder",
                                            tint = iconColor,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activity.horaInicio,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Badge(
                                containerColor = when (activity.type) {
                                    StopType.START -> MaterialTheme.colorScheme.tertiaryContainer
                                    StopType.FOOD -> Color(0xFFFFE0B2)
                                    StopType.PLACE -> MaterialTheme.colorScheme.primaryContainer
                                }
                            ) {
                                Text(
                                    text = activity.duracionEstimada,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activity.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activity.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Reason / Sugerencia
                        if (activity.reason.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = getIconForVisualType(activity.visualType),
                                    contentDescription = "Tipo de actividad",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = activity.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Costo: ${activity.costoEstimado}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (activity.latitud != null && activity.longitud != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Ubicación",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Lat: ${"%.4f".format(activity.latitud)}, Lon: ${"%.4f".format(activity.longitud)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun getIconForVisualType(visualType: ActivityVisualType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (visualType) {
        ActivityVisualType.START -> Icons.Default.Place
        ActivityVisualType.FOOD -> Icons.Default.Info
        ActivityVisualType.NATURE -> Icons.Default.Place
        ActivityVisualType.ADVENTURE -> Icons.Default.LocationOn
        ActivityVisualType.CULTURE -> Icons.Default.Star
        ActivityVisualType.HISTORY -> Icons.Default.Star
        ActivityVisualType.SHOPPING -> Icons.Default.Star
        ActivityVisualType.PHOTO -> Icons.Default.Star
        ActivityVisualType.EVENT -> Icons.Default.DateRange
        ActivityVisualType.FAMILY -> Icons.Default.Person
        ActivityVisualType.MAINSTREAM -> Icons.Default.Star
        ActivityVisualType.DEFAULT -> Icons.Default.LocationOn
    }
}

@Composable
private fun getPlaceholderColors(visualType: ActivityVisualType): Pair<Color, Color> {
    return when (visualType) {
        ActivityVisualType.NATURE, ActivityVisualType.ADVENTURE -> 
            Color(0xFFE8F5E9) to Color(0xFF2E7D32) // Verde suave
        ActivityVisualType.FOOD -> 
            Color(0xFFFFF3E0) to Color(0xFFEF6C00) // Naranja cálido
        ActivityVisualType.CULTURE, ActivityVisualType.HISTORY -> 
            Color(0xFFE3F2FD) to Color(0xFF1565C0) // Azul suave
        else -> 
            Color(0xFFF5F5F5) to Color(0xFF757575) // Gris neutro
    }
}
