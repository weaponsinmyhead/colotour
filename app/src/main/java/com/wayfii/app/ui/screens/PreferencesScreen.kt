package com.wayfii.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wayfii.app.data.model.*
import com.wayfii.app.data.repository.NominatimGeocodingRepository
import com.wayfii.app.ui.viewmodel.PreferencesViewModel
import com.google.android.gms.location.LocationServices
import java.util.Locale

// ── Theme Design Palette ───────────────────────────────────────────────────
private val ScreenBackground = Color(0xFFF8F9FA)
private val CardBackground = Color.White
private val BorderColor = Color(0xFFE2E8F0)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)
private val TealPrimary = Color(0xFF00897B)
private val TealSelectedFill = Color(0xFFE0F2F1)
private val CoralCTA = Color(0xFFFF5A5F)

// ── Vibe Cards Data ────────────────────────────────────────────────────────
private data class VibeItem(
    val interest: TourismInterest,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val VIBE_ITEMS = listOf(
    VibeItem(TourismInterest.CLASICO, "Clásico", "Imperdibles del lugar", Icons.Default.Star),
    VibeItem(TourismInterest.NATURALEZA, "Naturaleza", "Aire libre y paisajes", Icons.Default.Place),
    VibeItem(TourismInterest.GASTRONOMICO, "Gastronomía", "Sabores locales", Icons.Default.Favorite),
    VibeItem(TourismInterest.AVENTURA, "Aventura", "Acción y trekking", Icons.Default.LocationOn),
    VibeItem(TourismInterest.CULTURAL, "Cultural", "Arte e historia", Icons.Default.Person)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    onGenerate: (TravelPreferences) -> Unit,
    onOpenJournal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefViewModel: PreferencesViewModel = viewModel {
        PreferencesViewModel(NominatimGeocodingRepository())
    }
    
    val currentLocationName by prefViewModel.currentLocationName.collectAsState()
    val isResolvingLocation by prefViewModel.isResolvingLocation.collectAsState()

    // ── Form State ───────────────────────────────────────────────────────────
    var useCurrentLocation by remember { mutableStateOf(false) }
    var currentCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var destino by remember { mutableStateOf("") }
    var startingPointName by remember { mutableStateOf("") }

    val intereses = remember { mutableStateListOf(TourismInterest.CLASICO, TourismInterest.NATURALEZA) }
    val movilidad = remember { mutableStateListOf(MobilityType.CAMINANDO) }

    var timeRange by remember { mutableStateOf(540f..1080f) }   // 09:00–18:00
    var includeFoodStops by remember { mutableStateOf(true) }
    var cantidadPersonas by remember { mutableFloatStateOf(1f) }
    var presupuesto by remember { mutableStateOf(BudgetLevel.MEDIO) }
    var ritmo by remember { mutableStateOf(TravelPace.EQUILIBRADO) }

    var showAdvanced by remember { mutableStateOf(false) }

    // ── Context Simulation State ─────────────────────────────────────────────
    var useCustomContext by remember { mutableStateOf(false) }
    var selectedSeason by remember { mutableStateOf(Season.WINTER) }
    var selectedTimeOfDay by remember { mutableStateOf(TimeOfDay.AFTERNOON) }
    var selectedWeather by remember { mutableStateOf(WeatherCondition.SUNNY) }
    var selectedTemperature by remember { mutableFloatStateOf(18f) }
    var showContextDetails by remember { mutableStateOf(false) }

    var showDestinoError by remember { mutableStateOf(false) }
    var showInteresError by remember { mutableStateOf(false) }
    var showMovilidadError by remember { mutableStateOf(false) }

    // Location provider
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            useCurrentLocation = true
            obtenerUbicacionActual(fusedLocationClient) { lat, lon ->
                currentCoords = lat to lon
                prefViewModel.resolveLocationName(lat, lon)
                destino = "" 
            }
        } else {
            useCurrentLocation = false
        }
    }

    // Dynamic Summary Texts
    val startFormatted = formatMinutes(timeRange.start.toInt())
    val endFormatted   = formatMinutes(timeRange.endInclusive.toInt())
    
    val interestsCount = intereses.size
    val mainMobility = movilidad.firstOrNull()?.descripcion ?: "Caminando"
    val bottomSummaryText = "$startFormatted–$endFormatted · $interestsCount interés${if (interestsCount != 1) "es" else ""} · $mainMobility"

    val mobilitySummary = if (movilidad.isNotEmpty()) movilidad.joinToString(", ") { it.descripcion } else "Sin movilidad"
    val budgetSummary = presupuesto.descripcion
    val peopleCountInt = cantidadPersonas.toInt()
    val peopleSummary = "$peopleCountInt persona${if (peopleCountInt > 1) "s" else ""}"
    val foodSummary = if (includeFoodStops) "Con comida" else "Sin comida"
    val collapsedAdvancedSummary = "$mobilitySummary · $budgetSummary · $peopleSummary · $foodSummary"

    val secondaryInterests = remember {
        TourismInterest.entries.filterNot { vibe -> VIBE_ITEMS.any { it.interest == vibe } }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        // ── Main Scrollable Content ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Top App Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Wayfii",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Descubrí tu próxima aventura",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Surface(
                    onClick = onOpenJournal,
                    shape = RoundedCornerShape(16.dp),
                    color = TealPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "📖 Diario", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TealPrimary)
                    }
                }
            }

            // 2. Main Search Card (Destination Protagonist)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, BorderColor),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Mode Switcher: Buscar vs Mi Ubicación
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SearchTabButton(
                            text = "Buscar Ciudad",
                            icon = Icons.Default.Search,
                            selected = !useCurrentLocation,
                            onClick = { useCurrentLocation = false },
                            modifier = Modifier.weight(1f)
                        )
                        SearchTabButton(
                            text = "Mi Ubicación",
                            icon = Icons.Default.LocationOn,
                            selected = useCurrentLocation,
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
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = "DESTINO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        letterSpacing = 1.2.sp
                    )

                    if (useCurrentLocation) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TealSelectedFill.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isResolvingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = TealPrimary
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = TealPrimary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Ubicación actual",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Column {
                                Text(
                                    text = currentLocationName ?: "Obteniendo ubicación...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Explorar paradas turísticas a mi alrededor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = destino,
                            onValueChange = {
                                destino = it
                                if (showDestinoError && it.isNotBlank()) showDestinoError = false
                            },
                            label = { Text("¿A dónde viajás?") },
                            placeholder = { Text("Ciudad o destino") },
                            isError = showDestinoError,
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = if (showDestinoError) MaterialTheme.colorScheme.error else TealPrimary
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = Color(0xFFFAFAFA),
                                unfocusedContainerColor = Color(0xFFFAFAFA)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (showDestinoError) {
                            Text(
                                text = "Elegí un destino para continuar",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // Secondary Input: Point of Origin
                    OutlinedTextField(
                        value = startingPointName,
                        onValueChange = { startingPointName = it },
                        label = { Text("Desde dónde empezás (opcional)") },
                        placeholder = { Text("Hotel, estación o zona") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2.5 Context Engine Card (Moment, Season & Weather)
            ContextSelectionCard(
                useCustomContext = useCustomContext,
                selectedSeason = selectedSeason,
                selectedTimeOfDay = selectedTimeOfDay,
                selectedWeather = selectedWeather,
                selectedTemperature = selectedTemperature,
                showDetails = showContextDetails,
                onToggleCustomContext = { useCustomContext = it },
                onToggleDetails = { showContextDetails = !showContextDetails },
                onSeasonSelect = { selectedSeason = it },
                onTimeSelect = { selectedTimeOfDay = it },
                onWeatherSelect = { selectedWeather = it },
                onTempChange = { selectedTemperature = it }
            )

            // 3. Compact Date/Time Selector ("Cuándo")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, BorderColor),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cuándo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimePill(timeText = startFormatted)
                            Text("–", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            TimePill(timeText = endFormatted)
                        }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = TealPrimary,
                            inactiveTrackColor = Color(0xFFE2E8F0),
                            thumbColor = TealPrimary
                        )
                    )
                }
            }

            // 4. "Travel Vibe" Cards ("Qué tipo de viaje querés")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Qué tipo de viaje querés",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (showInteresError && intereses.isEmpty()) {
                    Text(
                        text = "Seleccioná al menos un estilo de viaje",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Grid of Vibe Cards
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val chunkedVibes = VIBE_ITEMS.chunked(2)
                    chunkedVibes.forEach { rowVibes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowVibes.forEach { vibe ->
                                val isSelected = intereses.contains(vibe.interest)
                                VibeCard(
                                    vibe = vibe,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (isSelected) intereses.remove(vibe.interest) else intereses.add(vibe.interest)
                                        if (intereses.isNotEmpty()) showInteresError = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowVibes.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 5. Advanced Customization ("Personalizar más" collapsed by default)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardBackground,
                border = BorderStroke(1.dp, BorderColor),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvanced = !showAdvanced }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Personalizar aventura",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (!showAdvanced) {
                                Text(
                                    text = collapsedAdvancedSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (showAdvanced) "Colapsar opciones" else "Expandir opciones",
                            tint = TealPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = showAdvanced,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            HorizontalDivider(color = BorderColor)

                            // Additional Interests
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Intereses adicionales",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    secondaryInterests.forEach { interest ->
                                        val isSelected = intereses.contains(interest)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) intereses.remove(interest) else intereses.add(interest)
                                                if (intereses.isNotEmpty()) showInteresError = false
                                            },
                                            label = { Text(interest.descripcion) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TealSelectedFill,
                                                selectedLabelColor = TealPrimary,
                                                containerColor = Color(0xFFF1F5F9),
                                                labelColor = TextPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = BorderColor,
                                                selectedBorderColor = TealPrimary,
                                                enabled = true,
                                                selected = isSelected
                                            )
                                        )
                                    }
                                }
                            }

                            // Mobility Selector
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Movilidad",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                if (showMovilidadError && movilidad.isEmpty()) {
                                    Text(
                                        text = "Seleccioná al menos un medio de transporte",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                FlowRow(
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
                                                selectedContainerColor = TealSelectedFill,
                                                selectedLabelColor = TealPrimary,
                                                containerColor = Color(0xFFF1F5F9),
                                                labelColor = TextPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = FilterChipDefaults.filterChipBorder(
                                                borderColor = BorderColor,
                                                selectedBorderColor = TealPrimary,
                                                enabled = true,
                                                selected = isSelected
                                            )
                                        )
                                    }
                                }
                            }

                            // Budget Selector
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Presupuesto",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BudgetLevel.entries.forEach { level ->
                                        val isSelected = presupuesto == level
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { presupuesto = level },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) TealSelectedFill else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSelected) TealPrimary else BorderColor)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = level.descripcion,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) TealPrimary else TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // People Counter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Personas",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Tamaño del grupo de viaje",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { if (cantidadPersonas > 1f) cantidadPersonas -= 1f },
                                        enabled = cantidadPersonas > 1f,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFF1F5F9), CircleShape)
                                    ) {
                                        Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    }
                                    Text(
                                        text = "${cantidadPersonas.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    IconButton(
                                        onClick = { if (cantidadPersonas < 10f) cantidadPersonas += 1f },
                                        enabled = cantidadPersonas < 10f,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFF1F5F9), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Sumar persona", tint = TextPrimary)
                                    }
                                }
                            }

                            // Food Stops Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Paradas para comer",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Incluir restaurantes y cafés",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = includeFoodStops,
                                    onCheckedChange = { includeFoodStops = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = TealPrimary,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }

                            // Travel Pace Selector
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Ritmo de viaje",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TravelPace.entries.forEach { p ->
                                        val isSelected = ritmo == p
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { ritmo = p },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) TealSelectedFill else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSelected) TealPrimary else BorderColor)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = p.descripcion,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) TealPrimary else TextPrimary
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

        // 6. Floating Bottom CTA Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = CardBackground,
            border = BorderStroke(1.dp, BorderColor),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = bottomSummaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Button(
                    onClick = {
                        val hasDestino   = useCurrentLocation || destino.isNotBlank()
                        val hasIntereses = intereses.isNotEmpty()
                        val hasMovilidad = movilidad.isNotEmpty()

                        showDestinoError   = !hasDestino
                        showInteresError   = !hasIntereses
                        showMovilidadError = !hasMovilidad

                        if (hasDestino && hasIntereses && hasMovilidad) {
                            val contextOverridePayload = if (useCustomContext) {
                                ContextEnvironment(
                                    season = selectedSeason,
                                    timeOfDay = selectedTimeOfDay,
                                    weather = selectedWeather,
                                    temperatureCelsius = selectedTemperature.toInt(),
                                    cityName = if (useCurrentLocation) (currentLocationName ?: "la ciudad") else destino
                                )
                            } else null

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
                                    lon                = if (useCurrentLocation) currentCoords?.second else null,
                                    contextOverride    = contextOverridePayload
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralCTA,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Encontrar Aventuras",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Private Helper Composables ──────────────────────────────────────────────

@Composable
private fun SearchTabButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) CardBackground else Color.Transparent,
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) TealPrimary else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) TextPrimary else TextSecondary
            )
        }
    }
}

@Composable
private fun TimePill(timeText: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = TealPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun VibeCard(
    vibe: VibeItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .semantics { contentDescription = "${vibe.title}: ${vibe.subtitle}" },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) TealSelectedFill else CardBackground,
        border = BorderStroke(1.5.dp, if (isSelected) TealPrimary else BorderColor),
        shadowElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) TealPrimary else Color(0xFFF1F5F9),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = vibe.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vibe.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) TealPrimary else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = vibe.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
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
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationFound(location.latitude, location.longitude)
            } else {
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
        // Location permission handled by launcher
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextSelectionCard(
    useCustomContext: Boolean,
    selectedSeason: Season,
    selectedTimeOfDay: TimeOfDay,
    selectedWeather: WeatherCondition,
    selectedTemperature: Float,
    showDetails: Boolean,
    onToggleCustomContext: (Boolean) -> Unit,
    onToggleDetails: () -> Unit,
    onSeasonSelect: (Season) -> Unit,
    onTimeSelect: (TimeOfDay) -> Unit,
    onWeatherSelect: (WeatherCondition) -> Unit,
    onTempChange: (Float) -> Unit
) {
    val currentBadgeText = if (useCustomContext) {
        "${selectedSeason.emoji} ${selectedSeason.displayName} · ${selectedWeather.emoji} ${selectedWeather.displayName} (${selectedTemperature.toInt()}°C) · ${selectedTimeOfDay.emoji} ${selectedTimeOfDay.displayName}"
    } else {
        "🌸 Invierno (Detectado) · ☀️ Soleado (18°C) · 🍃 Tarde"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, if (useCustomContext) TealPrimary.copy(alpha = 0.6f) else BorderColor),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "✨", fontSize = 16.sp)
                        Text(
                            text = "Momento & Clima",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Adaptá la historia al clima y la estación del año",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Switch(
                    checked = useCustomContext,
                    onCheckedChange = {
                        onToggleCustomContext(it)
                        if (it && !showDetails) onToggleDetails()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    )
                )
            }

            // Current Active Badge Pill
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (useCustomContext) TealSelectedFill else Color(0xFFF1F5F9),
                border = BorderStroke(0.5.dp, if (useCustomContext) TealPrimary else BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onToggleDetails() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentBadgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (useCustomContext) TealPrimary else TextPrimary
                    )
                    Icon(
                        imageVector = if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Context Options
            AnimatedVisibility(
                visible = showDetails,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(color = BorderColor)

                    // Season Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Estación del Año",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Season.entries.forEach { season ->
                                val isSelected = selectedSeason == season
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onToggleCustomContext(true)
                                            onSeasonSelect(season)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) TealSelectedFill else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) TealPrimary else BorderColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(text = season.emoji, fontSize = 16.sp)
                                        Text(
                                            text = season.displayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) TealPrimary else TextPrimary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Time of Day Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Momento del Día",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeOfDay.entries.forEach { tod ->
                                val isSelected = selectedTimeOfDay == tod
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onToggleCustomContext(true)
                                        onTimeSelect(tod)
                                    },
                                    label = { Text("${tod.emoji} ${tod.displayName}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealSelectedFill,
                                        selectedLabelColor = TealPrimary,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = BorderColor,
                                        selectedBorderColor = TealPrimary,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }

                    // Weather Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Condición Climática",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WeatherCondition.entries.forEach { wc ->
                                val isSelected = selectedWeather == wc
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onToggleCustomContext(true)
                                        onWeatherSelect(wc)
                                    },
                                    label = { Text("${wc.emoji} ${wc.displayName}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealSelectedFill,
                                        selectedLabelColor = TealPrimary,
                                        containerColor = Color(0xFFF1F5F9),
                                        labelColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = BorderColor,
                                        selectedBorderColor = TealPrimary,
                                        enabled = true,
                                        selected = isSelected
                                    )
                                )
                            }
                        }
                    }

                    // Temperature Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Temperatura Estimada",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${selectedTemperature.toInt()} °C",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary
                            )
                        }
                        Slider(
                            value = selectedTemperature,
                            onValueChange = {
                                onToggleCustomContext(true)
                                onTempChange(it)
                            },
                            valueRange = 0f..40f,
                            steps = 40,
                            colors = SliderDefaults.colors(
                                activeTrackColor = TealPrimary,
                                inactiveTrackColor = Color(0xFFE2E8F0),
                                thumbColor = TealPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

