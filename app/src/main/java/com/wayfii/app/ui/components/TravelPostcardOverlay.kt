package com.wayfii.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.wayfii.app.data.model.TravelPostcard

private val CardBg = Color(0xFFFAF9F6)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val TealPrimary = Color(0xFF00897B)
private val CoralAccent = Color(0xFFFF5A5F)

@Composable
fun TravelPostcardOverlay(
    postcard: TravelPostcard,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .shadow(20.dp),
            shape = RoundedCornerShape(28.dp),
            color = CardBg,
            border = BorderStroke(2.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── POSTCARD HERO COVER PHOTO WITH POSTAL STAMP OVERLAY ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    AsyncImage(
                        model = postcard.heroImageUrl,
                        contentDescription = postcard.locationName,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                        error = painterResource(id = com.wayfii.app.R.drawable.park_placeholder),
                        modifier = Modifier.fillMaxSize()
                    )

                    // Dark subtle gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )

                    // Postal Stamp Badge (Top Right)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, TealPrimary)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "POSTAL WAYFII",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = TealPrimary,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = postcard.unlockedDateText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Chapter Title Badge (Bottom Left)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = TealPrimary,
                        border = BorderStroke(0.5.dp, Color.White)
                    ) {
                        Text(
                            text = "✨ ${postcard.chapterTitle}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // ── POSTCARD STORY CONTENT ──
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = postcard.locationName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        fontSize = 24.sp
                    )

                    Text(
                        text = postcard.shortStory,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextDark.copy(alpha = 0.88f),
                        lineHeight = 22.sp
                    )
                }

                // ── HISTORICAL CURIOSITY / FUN FACT CARD ──
                if (postcard.funFact.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TealPrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "¿SABÍAS QUE...?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = TealPrimary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = postcard.funFact,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDark,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                // ── CONTINUATION CTA BUTTON ──
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralAccent,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continuar Aventura",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
