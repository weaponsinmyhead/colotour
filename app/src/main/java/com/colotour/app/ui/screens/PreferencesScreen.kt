package com.colotour.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.colotour.app.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onGenerate: (TravelPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    var destino by remember { mutableStateOf("") }
    var duracion by remember { mutableStateOf(Duracion.DIA_COMPLETO) }
    var movilidad by remember { mutableStateOf(Movilidad.CAMINANDO) }
    var cantidadPersonas by remember { mutableStateOf(1f) }
    var presupuesto by remember { mutableStateOf(Presupuesto.MEDIO) }
    var estiloTuristico by remember { mutableStateOf(EstiloTuristico.CULTURAL) }
    var ritmo by remember { mutableStateOf(Ritmo.EQUILIBRADO) }

    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Preferencias de tu Viaje",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Destino
        OutlinedTextField(
            value = destino,
            onValueChange = {
                destino = it
                if (showError && it.isNotBlank()) showError = false
            },
            label = { Text("¿A dónde viajas?") },
            placeholder = { Text("Ej. Buenos Aires, Bariloche...") },
            isError = showError,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (showError) {
            Text(
                text = "El destino no puede estar vacío",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Duración
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Duración del itinerario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Duracion.entries.forEach { item ->
                        FilterChip(
                            selected = duracion == item,
                            onClick = { duracion = item },
                            label = { Text(item.descripcion) }
                        )
                    }
                }
            }
        }

        // Movilidad
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Cómo te vas a mover",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Movilidad.entries.forEach { item ->
                        FilterChip(
                            selected = movilidad == item,
                            onClick = { movilidad = item },
                            label = { Text(item.descripcion) }
                        )
                    }
                }
            }
        }

        // Cantidad de personas
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

        // Presupuesto
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Presupuesto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Presupuesto.entries.forEach { item ->
                        FilterChip(
                            selected = presupuesto == item,
                            onClick = { presupuesto = item },
                            label = { Text(item.descripcion) }
                        )
                    }
                }
            }
        }

        // Estilo Turístico (Dropdown)
        var estiloDropdownExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = estiloDropdownExpanded,
            onExpandedChange = { estiloDropdownExpanded = !estiloDropdownExpanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = estiloTuristico.descripcion,
                onValueChange = {},
                label = { Text("Estilo Turístico") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = estiloDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(
                expanded = estiloDropdownExpanded,
                onDismissRequest = { estiloDropdownExpanded = false }
            ) {
                EstiloTuristico.entries.forEach { estilo ->
                    DropdownMenuItem(
                        text = { Text(estilo.descripcion) },
                        onClick = {
                            estiloTuristico = estilo
                            estiloDropdownExpanded = false
                        }
                    )
                }
            }
        }

        // Ritmo
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Ritmo del viaje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Ritmo.entries.forEach { item ->
                        FilterChip(
                            selected = ritmo == item,
                            onClick = { ritmo = item },
                            label = { Text(item.descripcion) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (destino.isBlank()) {
                    showError = true
                } else {
                    onGenerate(
                        TravelPreferences(
                            destino = destino,
                            duracion = duracion,
                            movilidad = movilidad,
                            cantidadPersonas = cantidadPersonas.toInt(),
                            presupuesto = presupuesto,
                            estiloTuristico = estiloTuristico,
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
