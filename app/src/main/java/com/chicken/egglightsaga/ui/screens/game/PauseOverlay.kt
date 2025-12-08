package com.chicken.egglightsaga.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chicken.egglightsaga.ui.screens.component.LuminousActionButton
import com.chicken.egglightsaga.ui.screens.component.OutlineActionButton

/* ---------- Публічний API ---------- */

@Composable
fun PauseOverlay(
    visible: Boolean,
    onResume: () -> Unit,
    onMenu: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            PauseCard(
                onResume = onResume,
                onMenu = onMenu
            )
        }
    }
}

/* ---------- Внутрішні елементи ---------- */

@Composable
private fun PauseCard(
    onResume: () -> Unit,
    onMenu: () -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .padding(50.dp)
            .shadow(
                elevation = 26.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color(0x5530E7C8),
                spotColor = Color(0x5530E7C8)
            )
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                clip = false,
                ambientColor = Color(0x3329CFB1),
                spotColor = Color(0x3329CFB1)
            )
            .clip(cardShape)
            .background(Color(0xFF0C1221))
            .border(2.dp, Color(0xFF29CFB1), cardShape)
            .padding(horizontal = 22.dp, vertical = 18.dp)
            .widthIn(min = 220.dp)
            .wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Заголовок
        Text(
            text = "PAUSE",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFFFB84D),
            modifier = Modifier
                .padding(top = 2.dp, bottom = 16.dp)
                .glowTextShadow(color = Color(0xFFFFB84D), radius = 10.dp),
            textAlign = TextAlign.Center
        )

        // Primary (зелена) кнопка
        LuminousActionButton(
            label = "RESUME",
            onClick = onResume,
            bg = Color(0xFF35E2C5),
            glow = Color(0x6635E2C5),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Outline (помаранчева) кнопка
        OutlineActionButton(
            label = "MENU",
            onActivate = onMenu,
            border = Color(0xFFFFB84D),
            glow = Color(0x66FFB84D),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ---------- Невеличкий шадоу для тексту (імітація сяйва) ---------- */

private fun Modifier.glowTextShadow(color: Color, radius: Dp) = this.shadow(
    elevation = radius, // просте та стабільне рішення без RenderEffect/BlurMaskFilter
    ambientColor = color.copy(alpha = 0.45f),
    spotColor = color.copy(alpha = 0.45f),
    clip = false,
    shape = RoundedCornerShape(0.dp)
)

/* ---------- Прев’ю ---------- */

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun PauseOverlay_Preview() {
    PauseOverlay(
        visible = true,
        onResume = {},
        onMenu = {}
    )
}
