package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AvatarHelper {

    private val avatarGradients = listOf(
        listOf(Color(0xFF00E5FF), Color(0xFF0072FF)), // Cyan
        listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)), // Violet
        listOf(Color(0xFFEC4899), Color(0xFFBE185D)), // Magenta
        listOf(Color(0xFF10B981), Color(0xFF047857)), // Emerald
        listOf(Color(0xFFF59E0B), Color(0xFFD97706)), // Amber
        listOf(Color(0xFFEF4444), Color(0xFFB91C1C))  // Red
    )

    fun getBrush(index: Int): Brush {
        val safeIndex = kotlin.math.abs(index) % avatarGradients.size
        val colors = avatarGradients[safeIndex]
        return Brush.linearGradient(colors)
    }

    @Composable
    fun Avatar(
        name: String,
        avatarIndex: Int = 0,
        size: Dp = 40.dp,
        modifier: Modifier = Modifier
    ) {
        val brush = getBrush(avatarIndex)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(brush),
            contentAlignment = Alignment.Center
        ) {
            val iconSize = size * 0.55f
            when (kotlin.math.abs(avatarIndex) % 6) {
                0 -> Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                1 -> Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                2 -> Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                3 -> Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                4 -> Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                5 -> Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(iconSize))
                else -> {
                    val initial = name.firstOrNull()?.uppercase() ?: "U"
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45f).sp
                    )
                }
            }
        }
    }
}
