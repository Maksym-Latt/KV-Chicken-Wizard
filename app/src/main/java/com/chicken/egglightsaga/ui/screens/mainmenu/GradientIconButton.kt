package com.chicken.egglightsaga.ui.screens.mainmenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun GradientIconButton(
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconDrawable: Int? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconTint: Color? = null
) {
    Box(
        modifier = modifier
            .size(84.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(0x00382478),
                spotColor = Color(0x00382478)
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x003C2B83), // Прозорий зверху
                        Color(0xFF3C2B83)  // Суцільний знизу
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.5.dp,
                color = Color(0xFF6C57E9),
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when {
            icon != null -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: Color.Unspecified, // Використовуємо tint тільки якщо вказано
                    modifier = Modifier.size(45.dp)
                )
            }

            iconDrawable != null -> {
                Icon(
                    painter = painterResource(id = iconDrawable),
                    contentDescription = null,
                    tint = iconTint ?:Color.Unspecified, // Використовуємо tint тільки якщо вказано
                    modifier = Modifier.size(45.dp)
                )
            }
        }
    }
}
