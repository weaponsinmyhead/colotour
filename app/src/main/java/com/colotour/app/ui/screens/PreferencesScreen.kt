package com.colotour.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colotour.app.data.model.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onGenerate: (TravelPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    var destino by remember { mutableStateOf("") }
    var startingPointName by remember { mutableStateOf("") }
    
    // Defaults reducen la fricción inicial
    val intereses = remember { mutableStateListOf(TourismInterest.CLASICO, TourismInterest.NATURALEZA) }
    val movilidad = remember { mutableStateListOf(MobilityType.CAMINANDO) }
    
    var timeRange by remember { mutableStateOf(540f..1080f) } // 09:00 a 18:00
    var includeFoodStops by remember { mutableStateOf(true) }
    var cantidadPersonas by remember { mutableStateOf(1f) }
    var presupuesto by remember { mutableStateOf(BudgetLevel.MEDIO) }
    var ritmo by remember { mutableStateOf(TravelPace.EQUILIBRADO) }

    var showDestinoError by remember { mutableStateOf(false) }
    var showInteresError by remember { mutableStateOf(false) }
    var showMovilidadError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Encabezado principal elegante
        Column {
            Text(
                text = "Colotour",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Armá tu día ideal en minutos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Bloque 1: Destino y Punto de Partida
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ruta y Destino",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = destino,
                onValueChange = {
                    destino = it
                    if (showDestinoError && it.isNotBlank()) showDestinoError = false
                },
                label = { Text("¿A dónde viajás?") },
                placeholder = { Text("Ej. Bariloche, Mendoza, Ushuaia...") },
                isError = showDestinoError,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            if (showDestinoError) {
                Text(
                    text = "Por favor, ingresá un destino",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = startingPointName,
                onValueChange = { startingPointName = it },
                label = { Text("Punto de partida (Opcional)") },
                placeholder = { Text("Ej. Hotel, Aeropuerto, Centro...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Bloque 2: Horarios del Día (RangeSlider)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val startFormatted = formatMinutes(timeRange.start.toInt())
            val endFormatted = formatMinutes(timeRange.endInclusive.toInt())
            
            Text(
                text = "Tu Jornada Horaria",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Disponibilidad: $startFormatted a $endFormatted",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            RangeSlider(
                value = timeRange,
                onValueChange = { range ->
                    if (range.endInclusive - range.start >= 120f) {
                        timeRange = range
                    }
                },
                valueRange = 480f..1380f,
                steps = 29,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    thumbColor = MaterialTheme.colorScheme.tertiary
                )
            )
            Text(
                text = "Mínimo 2 horas de duración requeridas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Bloque 3: Intereses
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Intereses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TourismInterest.values().forEach { interest ->
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
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            if (showInteresError) {
                Text(
                    text = "Seleccioná al menos un interés",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Bloque 4: Movilidad
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Movilidad Preferida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MobilityType.values().forEach { mob ->
                    val isSelected = movilidad.contains(mob)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) movilidad.remove(mob) else movilidad.add(mob)
                            if (movilidad.isNotEmpty()) showMovilidadError = false
                        },
                        label = { Text(mob.descripcion) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            if (showMovilidadError) {
                Text(
                    text = "Seleccioná al menos un tipo de movilidad",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Bloque 5: Presupuesto y Comida
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Presupuesto y Comida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BudgetLevel.values().forEach { level ->
                    FilterChip(
                        selected = presupuesto == level,
                        onClick = { presupuesto = level },
                        label = { Text(level.descripcion) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Incluir paradas para comer",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Planifica almuerzo/cena automáticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Switch(
                    checked = includeFoodStops,
                    onCheckedChange = { includeFoodStops = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            }
        }

        // Bloque 6: Personas y Ritmo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Detalles del Grupo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Column {
                Text(
                    text = "Cantidad de personas: ${cantidadPersonas.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = cantidadPersonas,
                    onValueChange = { cantidadPersonas = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ritmo sugerido",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TravelPace.values().forEach { pace ->
                        FilterChip(
                            selected = ritmo == pace,
                            onClick = { ritmo = pace },
                            label = { Text(pace.descripcion) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Botón principal Coral llamativo
        Button(
            onClick = {
                val hasDestino = destino.isNotBlank()
                val hasIntereses = intereses.isNotEmpty()
                val hasMovilidad = movilidad.isNotEmpty()

                showDestinoError = !hasDestino
                showInteresError = !hasIntereses
                showMovilidadError = !hasMovilidad

                if (hasDestino && hasIntereses && hasMovilidad) {
                    onGenerate(
                        TravelPreferences(
                            destino = destino,
                            intereses = intereses.toSet(),
                            movilidad = movilidad.toSet(),
                            startMinutes = timeRange.start.toInt(),
                            endMinutes = timeRange.endInclusive.toInt(),
                            startingPointName = startingPointName,
                            includeFoodStops = includeFoodStops,
                            cantidadPersonas = cantidadPersonas.toInt(),
                            presupuesto = presupuesto,
                            ritmo = ritmo
                        )
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Crear Itinerario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}
