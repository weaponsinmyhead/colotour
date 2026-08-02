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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wayfii.app.data.model.*

private val WarmBg = Color(0xFFFAF9F6)
private val CardBg = Color.White
private val BorderCol = Color(0xFFE2E8F0)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val TealPrimary = Color(0xFF00897B)
private val AmberAccent = Color(0xFFD97706)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JournalHomeScreen(
    entries: List<JournalEntry>,
    collections: List<ThematicCollection>,
    memories: List<JournalMemory>,
    onSelectEntry: (JournalEntry) -> Unit,
    onOpenStamps: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(CardBg, CircleShape)
                            .border(1.dp, BorderCol, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextDark
                        )
                    }

                    Column {
                        Text(
                            text = "MI DIARIO DE VIAJE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            letterSpacing = 1.4.sp
                        )
                        Text(
                            text = "Aventuras Guardadas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TextDark
                        )
                    }
                }

                // Stamps Action Button
                Surface(
                    onClick = onOpenStamps,
                    shape = RoundedCornerShape(16.dp),
                    color = TealPrimary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "Stamp Book 🛂", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TealPrimary)
                    }
                }
            }

            // Main Scrollable Editorial Stream
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. CITY DISCOVERY MAP CARD ("Has explorado el 18% de Buenos Aires")
                item {
                    CityDiscoveryMapCard(cityName = "Buenos Aires", exploredPercent = 18)
                }

                // 2. MEMORY REFLECTION CARD ("Hace un año hoy...")
                if (memories.isNotEmpty()) {
                    item {
                        JournalMemoriesCard(memories = memories)
                    }
                }

                // 3. PASSPORT STAMPS CAROUSEL
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sellos de Pasaporte",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Ver colección",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TealPrimary,
                                modifier = Modifier.clickable { onOpenStamps() }
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(entries) { entry ->
                                PassportStampChip(stamp = entry.passportStamp)
                            }
                        }
                    }
                }

                // 4. THEMATIC COLLECTIONS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Colecciones Temáticas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            collections.forEach { col ->
                                CollectionProgressBadge(collection = col, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 5. CHRONOLOGICAL MAGAZINE STREAM TITLE
                item {
                    Text(
                        text = "Ediciones Anteriores (${entries.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 6. EDITORIAL MAGAZINE CARDS LIST
                items(entries, key = { it.id }) { entry ->
                    JournalMagazineCard(
                        entry = entry,
                        onClick = { onSelectEntry(entry) }
                    )
                }
            }
        }
    }
}

// ── CITY DISCOVERY MAP CARD ──
@Composable
private fun CityDiscoveryMapCard(
    cityName: String,
    exploredPercent: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, BorderCol),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "MAPA DE DESCUBRIMIENTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Has explorado el $exploredPercent% de $cityName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = TealPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "🗺️", fontSize = 22.sp)
                    }
                }
            }

            // Progress Bar
            LinearProgressIndicator(
                progress = { (exploredPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = TealPrimary,
                trackColor = Color(0xFFE2E8F0)
            )

            // Neighborhood Highlights Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeighborhoodChip(name = "Palermo ✓", isExplored = true)
                NeighborhoodChip(name = "Recoleta ✓", isExplored = true)
                NeighborhoodChip(name = "San Telmo ✓", isExplored = true)
                NeighborhoodChip(name = "La Boca", isExplored = false)
            }
        }
    }
}

@Composable
private fun NeighborhoodChip(name: String, isExplored: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isExplored) TealPrimary.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
        border = BorderStroke(0.5.dp, if (isExplored) TealPrimary else BorderCol)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isExplored) TealPrimary else TextMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ── MEMORIES CARD ──
@Composable
private fun JournalMemoriesCard(memories: List<JournalMemory>) {
    val memory = memories.firstOrNull() ?: return
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = AmberAccent.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = memory.iconEmoji, fontSize = 20.sp)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = memory.title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = AmberAccent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = memory.subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = memory.dateTag,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ── PASSPORT STAMP CHIP ──
@Composable
private fun PassportStampChip(stamp: PassportStamp) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, BorderCol),
        shadowElevation = 2.dp,
        modifier = Modifier.width(130.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = stamp.emoji, fontSize = 28.sp)
            Text(
                text = stamp.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stamp.unlockedDateText,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
    }
}

// ── COLLECTION PROGRESS BADGE ──
@Composable
private fun CollectionProgressBadge(
    collection: ThematicCollection,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = BorderStroke(1.dp, BorderCol)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = collection.iconEmoji, fontSize = 22.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${collection.collectedCount} de ${collection.totalCount} descubiertos",
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ── JOURNAL MAGAZINE CARD ──
@Composable
private fun JournalMagazineCard(
    entry: JournalEntry,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .semantics { contentDescription = "Entrada de diario ${entry.adventureTitle}" },
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, BorderCol),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Hero Cover Photo (Occupies top height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                AsyncImage(
                    model = entry.heroImageUrl,
                    contentDescription = entry.adventureTitle,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                    error = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                    modifier = Modifier.fillMaxSize()
                )

                // Dark Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                // Date & Passport Stamp Overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "📅 ${entry.completionDateFormatted}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, TealPrimary),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = entry.passportStamp.emoji, fontSize = 20.sp)
                        }
                    }
                }

                // Title & Subtitle Overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = entry.weatherBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = entry.adventureTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }
            }

            // Bottom Content
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = entry.narrative,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = BorderCol)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        JournalChip(text = "🚶 ${entry.distanceWalkedText}")
                        JournalChip(text = "🕒 ${entry.durationText}")
                        JournalChip(text = "👣 ${entry.estimatedStepsText}")
                    }

                    Text(
                        text = "Ver historia ➔",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun JournalChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF1F5F9)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp
        )
    }
}
