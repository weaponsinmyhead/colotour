package com.wayfii.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayfii.app.data.model.*
import com.wayfii.app.data.repository.NominatimGeocodingRepository
import com.wayfii.app.ui.components.*
import com.wayfii.app.ui.viewmodel.PreferencesViewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    onGenerate: (TravelPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefViewModel: PreferencesViewModel = viewModel {
        PreferencesViewModel(NominatimGeocodingRepository())
    }
    
    val currentLocationName by prefViewModel.currentLocationName.collectAsState()
    val isResolvingLocation by prefViewModel.isResolvingLocation.collectAsState()

    // ── Estado ──────────────────────────────────────────────────────────────
    var useCurrentLocation by remember { mutableStateOf(false) }
    var currentCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var destino by remember { mutableStateOf("") }
    var startingPointName by remember { mutableStateOf("") }

    val intereses = remember { mutableStateListOf(TourismInterest.CLASICO, TourismInterest.NATURALEZA) }
    val movilidad = remember { mutableStateListOf(MobilityType.CAMINANDO) }

    var timeRange by remember { mutableStateOf(540f..1080f) }   // 09:00–18:00
    var includeFoodStops by remember { mutableStateOf(value = true) }
    var cantidadPersonas by remember { mutableFloatStateOf(1f) }
    var presupuesto by remember { mutableStateOf(BudgetLevel.MEDIO) }
    var ritmo by remember { mutableStateOf(TravelPace.EQUILIBRADO) }

    var showAdvanced by remember { mutableStateOf(false) }

    var showDestinoError by remember { mutableStateOf(false) }
    var showInteresError by remember { mutableStateOf(false) }
    var showMovilidadError by remember { mutableStateOf(false) }

    // Manejo de ubicación
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Permiso concedido
            useCurrentLocation = true
            obtenerUbicacionActual(fusedLocationClient) { lat, lon ->
                currentCoords = lat to lon
                prefViewModel.resolveLocationName(lat, lon)
                destino = "" 
            }
        } else {
            // Si lo deniega, volvemos a modo búsqueda
            useCurrentLocation = false
        }
    }

    // ── Resumen para el sticky bar ───────────────────────────────────────────
    val startFormatted = formatMinutes(timeRange.start.toInt())
    val endFormatted   = formatMinutes(timeRange.endInclusive.toInt())
    val summaryText = if (useCurrentLocation && currentLocationName != null) {
        "$currentLocationName · $startFormatted–$endFormatted"
    } else if (destino.isNotBlank()) {
        "$destino · $startFormatted–$endFormatted"
    } else {
        "Definí tu próximo destino"
    }

    // ── Layout principal ────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decoración de fondo (Sensación de mapa/cielo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.background)
                    )
                )
        )

        // Contenido scrolleable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ── Header Visual ─────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Wayfii",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "Diseñemos tu mapa de hoy",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Bloque Principal (Ubicación vs Destino Protagonista) ───────────────────
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    
                    // Selector de modo: Ubicación Actual o Buscar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton(
                            text = "Mi Ubicación",
                            icon = Icons.Default.Place,
                            selected = useCurrentLocation,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                useCurrentLocation = true
                                checkAndRequestLocation(context) {
                                    obtenerUbicacionActual(fusedLocationClient) { lat, lon ->
                                        currentCoords = lat to lon
                                        prefViewModel.resolveLocationName(lat, lon)
                                    }
                                } ?: requestPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                            }
                        )
                        TabButton(
                            text = "Buscar Ciudad",
                            icon = Icons.Default.Search,
                            selected = !useCurrentLocation,
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                useCurrentLocation = false
                            }
                        )
                    }

                    if (useCurrentLocation) {
                        // Vista cuando se usa ubicación actual
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isResolvingLocation) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = currentLocationName ?: "Obteniendo ubicación...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Explorar atractivos a mi alrededor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Entrada de destino tradicional
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel(text = "¿A dónde vamos?")
                            OutlinedTextField(
                                value = destino,
                                onValueChange = {
                                    destino = it
                                    if (showDestinoError && it.isNotBlank()) showDestinoError = false
                                },
                                placeholder = { Text("Ej. Bariloche, Mendoza…") },
                                isError = showDestinoError,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                leadingIcon = { Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    
                    if (showDestinoError && !useCurrentLocation) {
                        Text(
                            text = "Elegí un destino para continuar",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    ExpandableSection(
                        title = if (useCurrentLocation) "Punto de inicio diferente" else "Punto de salida específico",
                        expanded = startingPointName.isNotEmpty() || showAdvanced,
                        onToggle = { showAdvanced = !showAdvanced }
                    ) {
                        OutlinedTextField(
                            value = startingPointName,
                            onValueChange = { startingPointName = it },
                            placeholder = { Text("Hotel, Estación, Aeropuerto…") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            // ── Horario & Personas ────────────────────────────────────────
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(text = "Tu horario")
                    Text(
                        text = "$startFormatted - $endFormatted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                RangeSlider(
                    value = timeRange,
                    onValueChange = { range ->
                        if ((range.endInclusive - range.start) >= 120f) {
                            timeRange = range
                        }
                    },
                    valueRange = 420f..1380f,
                    steps = 32,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        thumbColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }

            // ── Cantidad de Personas ─────────────────────────────────────
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel(text = "¿Cuántos son?")
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = "${cantidadPersonas.toInt()} personas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Slider(
                        value = cantidadPersonas,
                        onValueChange = { cantidadPersonas = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.secondary,
                            thumbColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }

            // ── Intereses ────────────────────────────────────────────────
            SectionCard {
                SectionLabel(text = "¿Qué te apasiona?")
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TourismInterest.entries.forEach { interest ->
                        val isSelected = intereses.contains(interest)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) intereses.remove(interest) else intereses.add(interest)
                                if (intereses.isNotEmpty()) showInteresError = false
                            },
                            label = { Text(interest.descripcion) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            // ── Movilidad ────────────────────────────────────────────────────
            SectionCard {
                SectionLabel(text = "¿Cómo prefieres moverte?")

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MobilityType.entries.forEach { mob ->
                        val isSelected = movilidad.contains(mob)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) movilidad.remove(mob) else movilidad.add(mob)
                                if (movilidad.isNotEmpty()) showMovilidadError = false
                            },
                            label = { Text(mob.descripcion) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.secondary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        }

        // ── Sticky Action Bar (Lúdico) ────────────────────────────────────────────────
        StickyActionBar(
            summaryText = summaryText,
            buttonLabel = "¡Crear mi Mapa!",
            modifier = Modifier.align(Alignment.BottomCenter),
            onAction = {
                val hasDestino   = useCurrentLocation || destino.isNotBlank()
                val hasIntereses = intereses.isNotEmpty()
                val hasMovilidad = movilidad.isNotEmpty()

                showDestinoError   = !hasDestino
                showInteresError   = !hasIntereses
                showMovilidadError = !hasMovilidad

                if (hasDestino && hasIntereses && hasMovilidad) {
                    onGenerate(
                        TravelPreferences(
                            destino            = if (useCurrentLocation) (currentLocationName ?: "Mi ubicación") else destino,
                            intereses          = intereses.toSet(),
                            movilidad          = movilidad.toSet(),
                            startMinutes       = timeRange.start.toInt(),
                            endMinutes         = timeRange.endInclusive.toInt(),
                            startingPointName  = startingPointName,
                            includeFoodStops   = includeFoodStops,
                            cantidadPersonas   = cantidadPersonas.toInt(),
                            presupuesto        = presupuesto,
                            ritmo              = ritmo,
                            lat                = if (useCurrentLocation) currentCoords?.first else null,
                            lon                = if (useCurrentLocation) currentCoords?.second else null
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}

private fun checkAndRequestLocation(context: Context, onGranted: () -> Unit): Unit? {
    val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    
    return if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
        onGranted()
        Unit
    } else {
        null
    }
}

private fun obtenerUbicacionActual(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onLocationFound: (Double, Double) -> Unit
) {
    try {
        // Primero intentamos con lastLocation por rapidez
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationFound(location.latitude, location.longitude)
            } else {
                // Si lastLocation es null, pedimos una actualización fresca
                val priority = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
                fusedLocationClient.getCurrentLocation(priority, null)
                    .addOnSuccessListener { freshLocation ->
                        freshLocation?.let {
                            onLocationFound(it.latitude, it.longitude)
                        }
                    }
            }
        }
    } catch (e: SecurityException) {
        // No debería pasar si chequeamos antes
    }
}
