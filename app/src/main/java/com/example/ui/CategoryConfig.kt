package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryInfo(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

object CategoryConfig {
    val categories = listOf(
        CategoryInfo("Dining", Icons.Default.Restaurant, Color(0xFFFF5722)),       // Dark Orange/Coral
        CategoryInfo("Groceries", Icons.Default.ShoppingCart, Color(0xFF4CAF50)),    // Green
        CategoryInfo("Transit", Icons.Default.DirectionsCar, Color(0xFF2196F3)),     // Blue
        CategoryInfo("Shopping", Icons.Default.LocalMall, Color(0xFFE91E63)),       // Pink/Magenta
        CategoryInfo("Entertainment", Icons.Default.Tv, Color(0xFF9C27B0)),         // Purple
        CategoryInfo("Bills & Utilities", Icons.Default.Power, Color(0xFFFFC107)),  // Gold/Amber
        CategoryInfo("Investments", Icons.Default.TrendingUp, Color(0xFF00E676)),   // Bright Green/Teal
        CategoryInfo("Others", Icons.Default.Category, Color(0xFF9E9E9E))          // Gray
    )

    fun getCategoryInfo(name: String): CategoryInfo {
        return categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: CategoryInfo(name, Icons.Default.Category, Color(0xFF9E9E9E))
    }
}
