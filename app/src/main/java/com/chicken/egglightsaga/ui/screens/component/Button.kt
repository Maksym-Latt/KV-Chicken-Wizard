package com.chicken.egglightsaga.ui.screens.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
internal fun LuminousActionButton(
    label: String,
    onClick: () -> Unit,
    bg: Color = Color(0xFF35E2C5),
    glow: Color = Color(0x6635E2C5),
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    widthFraction: Float? = null,
    height: Dp = 44.dp,
    fontSize: TextUnit = 18.sp
) {
    val shape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .then(
                if (widthFraction != null)
                    Modifier.fillMaxWidth(widthFraction)
                else Modifier
            )
            .height(height)
            .shadow(
                elevation = 22.dp, shape = shape, clip = false,
                ambientColor = glow, spotColor = glow
            )
            .shadow(
                elevation = 10.dp, shape = shape, clip = false,
                ambientColor = glow.copy(alpha = 0.6f), spotColor = glow.copy(alpha = 0.6f)
            )
            .clip(shape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
fun OutlineActionButton(
    label: String,
    onActivate: () -> Unit,
    border: Color = Color(0xFFFFB84D),
    glow: Color = Color(0x66FFB84D),
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    widthFraction: Float? = null,
    height: Dp = 44.dp,
    fontSize: TextUnit = 18.sp
) {
    val shape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .then(
                if (widthFraction != null)
                    Modifier.fillMaxWidth(widthFraction)
                else Modifier
            )
            .height(height)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                clip = false,
                ambientColor = glow,
                spotColor = glow
            )
            .clip(shape)
            .background(Color.Transparent)
            .border(2.dp, border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onActivate() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = border
        )
    }
}