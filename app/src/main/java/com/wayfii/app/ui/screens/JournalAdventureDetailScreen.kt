package com.wayfii.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wayfii.app.data.model.JournalEntry

private val WarmBg = Color(0xFFFAF9F6)
private val CardBg = Color.White
private val BorderCol = Color(0xFFE2E8F0)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val TealPrimary = Color(0xFF00897B)
private val AmberAccent = Color(0xFFD97706)

@Composable
fun JournalAdventureDetailScreen(
    entry: JournalEntry,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var userNotes by remember { mutableStateOf(entry.personalNotes) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // ── HERO COVER PHOTO (38% screen height) ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    AsyncImage(
                        model = entry.heroImageUrl,
                        contentDescription = entry.adventureTitle,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                        error = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                        modifier = Modifier.fillMaxSize()
                    )

                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.75f)
                                    )
                                )
                            )
                    )

                    // Floating Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al diario",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Passport Stamp Badge (Bottom Right Overlay)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, TealPrimary),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = entry.passportStamp.emoji, fontSize = 22.sp)
                            Column {
                                Text(
                                    text = entry.passportStamp.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = TealPrimary
                                )
                                Text(
                                    text = entry.passportStamp.unlockedDateText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── OVERLAPPING EDITORIAL PAGE CONTAINER ──
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-28).dp)
                        .shadow(16.dp, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = WarmBg
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Weather & Date Header Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = TealPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(0.5.dp, TealPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${entry.weatherBadge} · ${entry.completionDateFormatted}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        // Title & City
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = entry.cityName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.4.sp
                            )
                            Text(
                                text = entry.adventureTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextDark,
                                fontSize = 30.sp,
                                lineHeight = 36.sp
                            )
                        }

                        // Stat Chips (Distance, Duration, Steps, Rating)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DetailChip(icon = "🚶", text = entry.distanceWalkedText)
                            DetailChip(icon = "🕒", text = entry.durationText)
                            DetailChip(icon = "👣", text = entry.estimatedStepsText)
                            DetailChip(icon = "⭐", text = "5.0")
                        }

                        // Narrative Chapter Story
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, BorderCol),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "LA EXPERIENCIA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = TealPrimary,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = entry.narrative,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextDark,
                                    lineHeight = 25.sp
                                )
                            }
                        }

                        // ── VISITED CHAPTERS TIMELINE ──
                        Text(
                            text = "Capítulos Visitados (${entry.mainQuestStops.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }
            }

            // Visited Chapters Timeline Items
            items(entry.mainQuestStops.size) { index ->
                val stop = entry.mainQuestStops[index]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-28).dp)
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = CardBg,
                        border = BorderStroke(1.dp, BorderCol),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TealPrimary,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "✓", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Capítulo ${stop.order}: ${stop.titulo.replace("Inicio: ", "")}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = stop.descripcion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // ── USER PHOTOS PLACEHOLDER ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-14).dp)
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "📷 Tus Fotos de la Aventura",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, BorderCol),
                                modifier = Modifier
                                    .size(130.dp)
                                    .clickable { /* Add photo placeholder action */ }
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar foto", tint = TealPrimary)
                                    Text("Agregar Foto", style = MaterialTheme.typography.labelSmall, color = TealPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        item {
                            AsyncImage(
                                model = entry.heroImageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                        }
                    }
                }
            }

            // ── PERSONAL NOTES PLACEHOLDER ──
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, BorderCol)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📝 Notas & Recuerdos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Icon(Icons.Default.Edit, contentDescription = "Editar nota", tint = TealPrimary, modifier = Modifier.size(18.dp))
                        }

                        OutlinedTextField(
                            value = userNotes,
                            onValueChange = { userNotes = it },
                            placeholder = { Text("Escribí tus recuerdos personales sobre este viaje...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TealPrimary,
                                unfocusedBorderColor = BorderCol,
                                focusedContainerColor = WarmBg,
                                unfocusedContainerColor = WarmBg
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(icon: String, text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardBg,
        border = BorderStroke(1.dp, BorderCol)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = icon, fontSize = 12.sp)
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}
