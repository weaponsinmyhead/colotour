package com.colotour.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    
    // Defaults inteligentes reducen fricción inicial
    val intereses = remember { mutableStateListOf(TourismInterest.CLASICO, TourismInterest.NATURALEZA) }
    val movilidad = remember { mutableStateListOf(MobilityType.CAMINANDO) }
    
    var timeRange by remember { mutableStateOf(540f..1080f) } // 09:00 a 18:00
    var includeFoodStops by remember { mutableStateOf(true) }
    var cantidadPersonas by remember { mutableStateOf(1f) }
    var presupuesto by remember { mutableStateOf(BudgetLevel.MEDIO) }
    var ritmo by remember { mutableStateOf(TravelPace.EQUILIBRADO) }

    // Control de sección avanzada colapsable
    var showAdvanced by remember { mutableStateOf(false) }

    var showDestinoError by remember { mutableStateOf(false) }
    var showInteresError by remember { mutableStateOf(false) }
    var showMovilidadError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Formulario Scrolleable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 140.dp), // Margen inferior para no solaparse con la barra pegajosa
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
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

            // Datos Principales (Destino, Partida, Horario)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "¿A dónde viajás?",
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
                    label = { Text("Escribí tu destino") },
                    placeholder = { Text("Ej. Bariloche, Mendoza, Buenos Aires...") },
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
                    label = { Text("¿Desde dónde empezás? (Opcional)") },
                    placeholder = { Text("Ej. Hotel, Aeropuerto, Estación...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Horario
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val startFormatted = formatMinutes(timeRange.start.toInt())
                val endFormatted = formatMinutes(timeRange.endInclusive.toInt())

                Text(
                    text = "¿En qué horario estás disponible?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$startFormatted a $endFormatted",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

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
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        thumbColor = MaterialTheme.colorScheme.tertiary
                    )
                )
            }

            // Botón para expandir preferencias avanzadas
            OutlinedButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (showAdvanced) "Ocultar preferencias" else "Personalizar preferencias (Intereses, ritmo, etc.)",
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (showAdvanced) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Mostrar"
                    )
                }
            }

            // Sección avanzada con animación
            AnimatedVisibility(
                visible = showAdvanced,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Intereses
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "¿Qué te gustaría hacer hoy?",
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
                                    label = { Text(interest.descripcion) }
                                )
                            }
                        }
                        if (showInteresError) {
                            Text(
                                text = "Elegí al menos un interés",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Movilidad
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "¿Cómo te vas a mover?",
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
                                    label = { Text(mob.descripcion) }
                                )
                            }
                        }
                        if (showMovilidadError) {
                            Text(
                                text = "Elegí al menos un medio de movilidad",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Presupuesto y comida
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "¿Cuánto querés gastar?",
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
                                    modifier = Modifier.weight(1f)
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
                                    text = "Planifica desayunos, almuerzos o cenas",
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

                    // Acompañantes y Ritmo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "¿Con quiénes viajás?",
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
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar con resumen y botón de creación
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resumen Dinámico ligero
                val destinationSummary = if (destino.isNotBlank()) destino else "Escribí un destino"
                val mobilitySummary = movilidad.joinToString("/") { it.descripcion }
                val summaryText = "$destinationSummary • $mobilitySummary • Presupuesto ${presupuesto.descripcion} • ${cantidadPersonas.toInt()} pers."
                
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

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
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Armá mi recorrido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return String.format(Locale.getDefault(), "%02d:%02d", h, m)
}
