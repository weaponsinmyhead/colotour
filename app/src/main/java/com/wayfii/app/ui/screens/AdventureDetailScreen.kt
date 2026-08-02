package com.wayfii.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wayfii.app.data.model.AdventureProposal
import com.wayfii.app.data.model.ItineraryStop
import com.wayfii.app.data.model.SideQuestItem

// Premium Editorial Color Palette
private val WarmBg = Color(0xFFFAF9F6)
private val CardBg = Color.White
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val TealAccentStart = Color(0xFF00897B)
private val TealAccentEnd = Color(0xFF00BFA5)
private val TealBorder = Color(0xFFB2DFDB)
private val LineTeal = Color(0xFF00897B)
private val CardBorder = Color(0xFFF1F5F9)
private val FavoriteRed = Color(0xFFFF4D4D)

@Composable
fun AdventureDetailScreen(
    proposal: AdventureProposal,
    onBack: () -> Unit,
    onStartAdventure: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(proposal.isFavorite) }
    var expandedStopOrders by remember { mutableStateOf(setOf<Int>()) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    // Parallax calculation based on scroll offset
    val scrollOffset = remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            } else {
                400f
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBg)
    ) {
        // Main Scrollable Content
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp) // Room for sticky bottom CTA
        ) {
            // ── HERO IMAGE HEADER (38% screen height) ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    // Parallax Hero Image
                    AsyncImage(
                        model = proposal.heroImageUrl ?: proposal.imageResId,
                        contentDescription = proposal.title,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = proposal.imageResId),
                        error = painterResource(id = proposal.imageResId),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = scrollOffset.value * 0.45f
                            }
                    )

                    // Subtle dark gradient overlay to improve top controls visibility & mood
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                }
            }

            // ── OVERLAPPING ADVENTURE CARD CONTAINER ──
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-32).dp)
                        .shadow(16.dp, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = WarmBg,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Category & Context Badges
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = TealAccentStart.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, TealBorder)
                            ) {
                                Text(
                                    text = proposal.category.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TealAccentStart,
                                    letterSpacing = 1.4.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            proposal.adventureDna?.let { dna ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFFFFE0B2),
                                    border = BorderStroke(0.5.dp, Color(0xFFFFB74D))
                                ) {
                                    Text(
                                        text = dna.badgeText,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Title
                        Text(
                            text = proposal.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark,
                            fontSize = 30.sp,
                            lineHeight = 36.sp
                        )

                        // Quick Info Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            proposal.adventureScore?.let { score ->
                                QuickInfoChip(icon = "⚡", text = "${score.totalScore.toInt()} pts")
                            }
                            QuickInfoChip(icon = "🕒", text = proposal.durationText)
                            QuickInfoChip(icon = "🚶", text = proposal.distanceText)
                            QuickInfoChip(icon = "🌿", text = proposal.difficulty)
                        }

                        // Introduction / Narrative Paragraph
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBg, RoundedCornerShape(18.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "LA EXPERIENCIA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TealAccentStart,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = if (proposal.introNarrative.isNotBlank()) proposal.introNarrative else proposal.tagline,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextDark.copy(alpha = 0.88f),
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        // ── SECTION: TU AVENTURA (Timeline Stops) ──
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tu aventura",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }
            }

            // ── VERTICAL TIMELINE STOPS ──
            items(proposal.mainQuestStops.size) { index ->
                val stop = proposal.mainQuestStops[index]
                val isLast = index == proposal.mainQuestStops.size - 1
                val isExpanded = expandedStopOrders.contains(stop.order)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-32).dp)
                        .padding(horizontal = 22.dp)
                ) {
                    TimelineStopRow(
                        stop = stop,
                        isLast = isLast,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            expandedStopOrders = if (isExpanded) {
                                expandedStopOrders - stop.order
                            } else {
                                expandedStopOrders + stop.order
                            }
                        }
                    )
                }
            }

            // ── SECTION: SIDE QUESTS (Miniature Adventures Carousel) ──
            if (proposal.sideQuests.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-16).dp)
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "✨", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "Side Quests",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Desvíos opcionales cerca de tu ruta",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Horizontal Scroll of Miniature Adventures
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(proposal.sideQuests, key = { it.id }) { sideQuest ->
                                SideQuestMiniCard(sideQuest = sideQuest)
                            }
                        }
                    }
                }
            }
        }

        // ── FLOATING TOP CONTROL BAR (Over Image) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ← Back Button
            FloatingIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                onClick = onBack
            )

            // Right actions: ♡ Favorite & ⋯ More
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // ♡ Favorite Button with Heart Animation
                FloatingIconButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Guardar en favoritos",
                    tint = if (isFavorite) FavoriteRed else Color.White,
                    onClick = { isFavorite = !isFavorite }
                )

                // ⋯ More Options Button
                Box {
                    FloatingIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Más opciones",
                        onClick = { showMoreMenu = true }
                    )

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Compartir Aventura") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = { showMoreMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Guardar Offline") },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                            onClick = { showMoreMenu = false }
                        )
                    }
                }
            }
        }

        // ── PRIMARY ACTION: STICKY BOTTOM CTA BUTTON ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WarmBg.copy(alpha = 0.0f),
                            WarmBg.copy(alpha = 0.85f),
                            WarmBg
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onStartAdventure,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, shape = CircleShape, spotColor = TealAccentStart),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(TealAccentStart, TealAccentEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Iniciar aventura",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            fontSize = 17.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── QUICK INFO CHIP ──
@Composable
private fun QuickInfoChip(icon: String, text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = icon, fontSize = 13.sp)
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

// ── FLOATING TOP ICON BUTTON ──
@Composable
private fun FloatingIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── TIMELINE STOP ROW WITH LEFT SQUARE PHOTO & EXPANDABLE DETAILS ──
@Composable
private fun TimelineStopRow(
    stop: ItineraryStop,
    isLast: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (isLast) 12.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Left Column: Circle Step Badge & Connecting Line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Step Number Circle Badge
            Surface(
                shape = CircleShape,
                color = CardBg,
                border = BorderStroke(2.dp, LineTeal),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${stop.order}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = LineTeal
                    )
                }
            }

            // Connecting Vertical Line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .padding(vertical = 4.dp)
                        .background(LineTeal.copy(alpha = 0.4f))
                )
            }
        }

        // Right Column: Premium Stop Card (Square photo on LEFT, text middle, chevron right)
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onToggleExpand() }
                .animateContentSize(),
            shape = RoundedCornerShape(20.dp),
            color = CardBg,
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. SQUARE PHOTO ON THE LEFT (Width == Height == 80dp)
                    AsyncImage(
                        model = stop.imageUrl,
                        contentDescription = stop.titulo,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                        error = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(WarmBg)
                    )

                    // 2. MIDDLE CONTENT (Name, Distance, Narrative teaser)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stop.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (stop.distanceFromPrevious.isNotBlank()) {
                            Text(
                                text = stop.distanceFromPrevious,
                                style = MaterialTheme.typography.labelSmall,
                                color = TealAccentStart,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = stop.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = if (isExpanded) 10 else 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }

                    // 3. CHEVRON ON THE FAR RIGHT
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // ── FUTURE EXPANSION RICH CONTENT (Revealed on Tap) ──
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(color = CardBorder)

                        // Historical Context
                        if (stop.historicalInfo.isNotBlank()) {
                            RichDetailRow(
                                icon = Icons.Default.Info,
                                title = "Rincón Histórico",
                                detail = stop.historicalInfo
                            )
                        }

                        // Opening Hours
                        if (stop.openingHours.isNotBlank()) {
                            RichDetailRow(
                                icon = Icons.Default.DateRange,
                                title = "Horarios de Visita",
                                detail = stop.openingHours
                            )
                        }

                        // Fun Fact
                        if (stop.funFact.isNotBlank()) {
                            RichDetailRow(
                                icon = Icons.Default.Star,
                                title = "Dato Curioso",
                                detail = stop.funFact
                            )
                        }

                        // Audio Guide Button
                        if (stop.audioGuideDuration.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TealAccentStart.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, TealAccentStart.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = TealAccentStart,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Guía de Audio disponible (${stop.audioGuideDuration})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TealAccentStart
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Reproducir audio",
                                        tint = TealAccentStart,
                                        modifier = Modifier.size(20.dp)
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

@Composable
private fun RichDetailRow(
    icon: ImageVector,
    title: String,
    detail: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TealAccentStart,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 17.sp
            )
        }
    }
}

// ── MINIATURE ADVENTURE CARD FOR SIDE QUESTS (Horizontal Scroll) ──
@Composable
private fun SideQuestMiniCard(sideQuest: SideQuestItem) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Large Thumbnail Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                AsyncImage(
                    model = sideQuest.imageUrl,
                    contentDescription = sideQuest.title,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = com.wayfii.app.R.drawable.streetart_placeholder),
                    error = painterResource(id = com.wayfii.app.R.drawable.streetart_placeholder),
                    modifier = Modifier.fillMaxSize()
                )

                // Time Badge Chip
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "⭐ ${sideQuest.walkingTimeText}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Title & Description
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = sideQuest.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sideQuest.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
