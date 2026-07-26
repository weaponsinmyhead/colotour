package com.colotour.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val intereses = remember { mutableStateListOf(TourismInterest.CLASICO) }
    val movilidad = remember { mutableStateListOf(MobilityType.CAMINANDO) }
    var timeRange by remember { mutableStateOf(540f..1080f) } // 09:00 a 18:00 por defecto
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Planifica tu Día de Viaje",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Destino
        OutlinedTextField(
            value = destino,
            onValueChange = {
                destino = it
                if (showDestinoError && it.isNotBlank()) showDestinoError = false
            },
            label = { Text("¿A dónde viajas?") },
            placeholder = { Text("Ej. Buenos Aires, Bariloche...") },
            isError = showDestinoError,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (showDestinoError) {
            Text(
                text = "El destino no puede estar vacío",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // 2. Punto de Partida
        OutlinedTextField(
            value = startingPointName,
            onValueChange = { startingPointName = it },
            label = { Text("Punto de partida (Opcional)") },
            placeholder = { Text("Ej. Hotel, dirección, estación o zona") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 3. Intereses Múltiples
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Intereses Turísticos (Elige al menos uno)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TourismInterest.values().forEach { interest ->
                        val isSelected = intereses.contains(interest)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    intereses.remove(interest)
                                } else {
                                    intereses.add(interest)
                                }
                                if (intereses.isNotEmpty()) showInteresError = false
                            },
                            label = { Text(interest.descripcion) }
                        )
                    }
                }
                if (showInteresError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Debes seleccionar al menos un interés",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 4. Movilidad Múltiple
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Movilidad preferida (Elige al menos una)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MobilityType.values().forEach { mob ->
                        val isSelected = movilidad.contains(mob)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    movilidad.remove(mob)
                                } else {
                                    movilidad.add(mob)
                                }
                                if (movilidad.isNotEmpty()) showMovilidadError = false
                            },
                            label = { Text(mob.descripcion) }
                        )
                    }
                }
                if (showMovilidadError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Debes seleccionar al menos una movilidad",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // 5. Horario Rango Slider (08:00 a 23:00 = 480 min a 1380 min)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                val startFormatted = formatMinutes(timeRange.start.toInt())
                val endFormatted = formatMinutes(timeRange.endInclusive.toInt())
                Text(
                    text = "Horario disponible: $startFormatted a $endFormatted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                RangeSlider(
                    value = timeRange,
                    onValueChange = { range ->
                        // Rango mínimo obligatorio de 2 horas (120 minutos)
                        if (range.endInclusive - range.start >= 120f) {
                            timeRange = range
                        }
                    },
                    valueRange = 480f..1380f,
                    steps = 29
                )
            }
        }

        // 6. Switch paradas para comer
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Incluir paradas para comer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Agrega desayuno, almuerzo o cena según tu horario.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = includeFoodStops,
                    onCheckedChange = { includeFoodStops = it }
                )
            }
        }

        // 7. Presupuesto
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Nivel de Presupuesto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BudgetLevel.values().forEach { level ->
                        FilterChip(
                            selected = presupuesto == level,
                            onClick = { presupuesto = level },
                            label = { Text(level.descripcion) }
                        )
                    }
                }
            }
        }

        // 8. Cantidad de personas
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Cantidad de personas: ${cantidadPersonas.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = cantidadPersonas,
                    onValueChange = { cantidadPersonas = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
        }

        // 9. Ritmo
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Ritmo sugerido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TravelPace.values().forEach { pace ->
                        FilterChip(
                            selected = ritmo == pace,
                            onClick = { ritmo = pace },
                            label = { Text(pace.descripcion) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "Crear Itinerario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}
