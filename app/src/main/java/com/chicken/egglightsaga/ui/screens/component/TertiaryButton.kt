package com.chicken.egglightsaga.ui.screens.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chicken.egglightsaga.R

@Composable
fun TertiaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 54.dp,
    corner: Dp = 12.dp,
    padding: Dp = 5.dp,
    backgroundColor: Color = Color(0xFF0E1535),
    borderColor: Color = Color(0xFF7AFEE6),
    glowColor: Color = Color(0x99382478),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val alpha = if (enabled) 1f else 0.55f

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .background(backgroundColor.copy(alpha = alpha), shape = shape)
            .border(width = 1.5.dp, color = borderColor.copy(alpha = alpha), shape = shape)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun PauseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TertiaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = 20.dp)
                    .background(Color(0xFF20E3B2))
            )
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = 20.dp)
                    .background(Color(0xFF20E3B2))
            )
        }
    }
}

@Composable
fun HomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TertiaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Image(
            painter = painterResource(R.drawable.ic_home),
            contentDescription = "Home",
            contentScale = ContentScale.Fit
        )
    }
}

