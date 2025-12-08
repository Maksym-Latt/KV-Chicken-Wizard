package com.chicken.egglightsaga.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chicken.egglightsaga.ui.screens.component.OutlineActionButton
import com.chicken.egglightsaga.ui.theme.EggGlow
import com.chicken.egglightsaga.ui.theme.GrimoireTypography

@Composable
fun SettingsDialog(
    state: SettingsUiState,
    onDismiss: () -> Unit,
    onMusicToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onVibrationToggle: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent,
            modifier = Modifier.shadow(
                elevation = 25.dp,
                shape = RoundedCornerShape(32.dp),
                clip = false,
                ambientColor = Color(0x66382478),
                spotColor = Color(0x66382478)
            )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1F184A),
                                Color(0xFF3C2B83)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = Color(0xFF6C57E9),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clip(RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Заголовок
                    Text(
                        text = "SETTINGS",
                        style = GrimoireTypography.headlineMedium.copy(
                            color = EggGlow,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    // Налаштування
                    SettingToggleRow(
                        title = "MUSIC",
                        description = "Background melodies",
                        checked = state.musicEnabled,
                        onCheckedChange = onMusicToggle
                    )

                    SettingToggleRow(
                        title = "SOUND EFFECTS",
                        description = "Spell sounds & effects",
                        checked = state.soundEnabled,
                        onCheckedChange = onSoundToggle
                    )

                    SettingToggleRow(
                        title = "VIBRATION",
                        description = "Haptic feedback",
                        checked = state.vibrationEnabled,
                        onCheckedChange = onVibrationToggle
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlineActionButton(
                        label = "CLOSE",
                        onActivate = onDismiss,
                        widthFraction = 1f,
                        height = 60.dp,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                clip = false,
                ambientColor = Color(0x33382478),
                spotColor = Color(0x33382478)
            )
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = Color(0x14FFFFFF),
                    shape = RoundedCornerShape(20.dp)
                )
                .border(
                    width = 1.5.dp,
                    color = Color(0xFF6C57E9).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    )
                }

                // Кастомний Switch у стилі гри
                Box(
                    modifier = Modifier
                        .size(width = 52.dp, height = 32.dp)
                        .background(
                            color = if (checked) Color(0xFF2CF7BB) else Color(0xFF6C57E9).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = if (checked) Color(0xFF1EBE8D) else Color(0xFF6C57E9),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onCheckedChange(!checked) },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = Color.White,
                                shape = CircleShape
                            )
                            .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                            .padding(4.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = CircleShape,
                                clip = false,
                                ambientColor = Color(0x33000000),
                                spotColor = Color(0x33000000)
                            )
                    )
                }
            }
        }
    }
}