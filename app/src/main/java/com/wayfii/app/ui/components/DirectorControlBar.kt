package com.wayfii.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardBg = Color.White
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val TealPrimary = Color(0xFF00897B)
private val AmberAccent = Color(0xFFEA580C)
private val BlueAccent = Color(0xFF2563EB)

@Composable
fun DirectorControlBar(
    isSimulatingRain: Boolean,
    isSimulatingSunset: Boolean,
    isSimulatingSurprise: Boolean,
    onToggleRain: () -> Unit,
    onToggleSunset: () -> Unit,
    onToggleSurprise: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = CardBg.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎬 ADVENTURE DIRECTOR (SIMULADOR EN VIVO)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = TealPrimary,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )

                if (isSimulatingRain || isSimulatingSunset || isSimulatingSurprise) {
                    Text(
                        text = "Resetear 🔄",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.clickable { onReset() }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DirectorSimChip(
                    label = "🌅 Atardecer",
                    isSelected = isSimulatingSunset,
                    activeColor = AmberAccent,
                    onClick = onToggleSunset,
                    modifier = Modifier.weight(1f)
                )

                DirectorSimChip(
                    label = "☔ Lluvia",
                    isSelected = isSimulatingRain,
                    activeColor = BlueAccent,
                    onClick = onToggleRain,
                    modifier = Modifier.weight(1f)
                )

                DirectorSimChip(
                    label = "✨ Sorpresa",
                    isSelected = isSimulatingSurprise,
                    activeColor = TealPrimary,
                    onClick = onToggleSurprise,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DirectorSimChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) activeColor else Color(0xFFF1F5F9),
        border = BorderStroke(0.5.dp, if (isSelected) activeColor else Color(0xFFCBD5E1))
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TextDark,
                fontSize = 11.sp
            )
        }
    }
}
