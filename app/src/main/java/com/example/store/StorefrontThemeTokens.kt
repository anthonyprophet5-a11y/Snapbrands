package com.example.store

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Shop
import com.example.data.model.StorefrontTheme

/**
 * Visual design tokens governing storefront presentation across themes.
 * Themes share the same data model and layout tree, styled via tokens.
 */
data class StorefrontThemeTokens(
    val theme: StorefrontTheme,
    val primaryColor: Color,
    val onPrimaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val onSurfaceColor: Color,
    val onSurfaceVariantColor: Color,
    val accentColor: Color,
    val borderColor: Color,
    val cardShape: Shape,
    val buttonShape: Shape,
    val chipShape: Shape,
    val borderWidth: Dp,
    val titleFontWeight: FontWeight,
    val bodyFontFamily: FontFamily,
    val isDarkBackground: Boolean,
    val heroPaddingVertical: Dp,
    val badgeStyleLabel: String
)

object StorefrontThemeResolver {

    /**
     * Resolves tokens based on the selected theme and any custom color overrides set by the seller.
     */
    fun resolve(shop: Shop): StorefrontThemeTokens {
        val theme = shop.theme

        fun parseColor(hex: String?, default: Color): Color {
            if (hex.isNullOrBlank()) return default
            return try {
                val clean = hex.removePrefix("#")
                val longVal = clean.toLong(16)
                if (clean.length == 6) {
                    Color(0xFF000000 or longVal)
                } else {
                    Color(longVal)
                }
            } catch (e: Exception) {
                default
            }
        }

        return when (theme) {
            StorefrontTheme.MINIMAL -> {
                val primary = parseColor(shop.primaryColor, Color(0xFF18181B)) // Obsidian
                val bg = parseColor(shop.backgroundColor, Color(0xFFFAFAFA))
                StorefrontThemeTokens(
                    theme = theme,
                    primaryColor = primary,
                    onPrimaryColor = Color.White,
                    secondaryColor = parseColor(shop.secondaryColor, Color(0xFF71717A)),
                    backgroundColor = bg,
                    surfaceColor = Color.White,
                    onSurfaceColor = Color(0xFF09090B),
                    onSurfaceVariantColor = Color(0xFF71717A),
                    accentColor = Color(0xFF2563EB),
                    borderColor = Color(0xFFE4E4E7),
                    cardShape = RoundedCornerShape(8.dp),
                    buttonShape = RoundedCornerShape(8.dp),
                    chipShape = RoundedCornerShape(6.dp),
                    borderWidth = 1.dp,
                    titleFontWeight = FontWeight.Bold,
                    bodyFontFamily = FontFamily.SansSerif,
                    isDarkBackground = false,
                    heroPaddingVertical = 36.dp,
                    badgeStyleLabel = "MINIMAL CLEAN"
                )
            }
            StorefrontTheme.BOLD -> {
                val primary = parseColor(shop.primaryColor, Color(0xFF4338CA)) // Electric Indigo
                val bg = parseColor(shop.backgroundColor, Color(0xFFFFFFFF))
                StorefrontThemeTokens(
                    theme = theme,
                    primaryColor = primary,
                    onPrimaryColor = Color.White,
                    secondaryColor = parseColor(shop.secondaryColor, Color(0xFF06B6D4)),
                    backgroundColor = bg,
                    surfaceColor = Color(0xFFF8FAFC),
                    onSurfaceColor = Color(0xFF0F172A),
                    onSurfaceVariantColor = Color(0xFF475569),
                    accentColor = Color(0xFFF97316),
                    borderColor = Color(0xFF0F172A),
                    cardShape = RoundedCornerShape(4.dp),
                    buttonShape = RoundedCornerShape(4.dp),
                    chipShape = RoundedCornerShape(4.dp),
                    borderWidth = 2.dp,
                    titleFontWeight = FontWeight.Black,
                    bodyFontFamily = FontFamily.SansSerif,
                    isDarkBackground = false,
                    heroPaddingVertical = 44.dp,
                    badgeStyleLabel = "HIGH CONTRAST BOLD"
                )
            }
            StorefrontTheme.LUXURY -> {
                val primary = parseColor(shop.primaryColor, Color(0xFFD4AF37)) // Champagne Gold
                val bg = parseColor(shop.backgroundColor, Color(0xFF0F0F12)) // Deep Warm Obsidian
                StorefrontThemeTokens(
                    theme = theme,
                    primaryColor = primary,
                    onPrimaryColor = Color(0xFF121214),
                    secondaryColor = parseColor(shop.secondaryColor, Color(0xFFA1A1AA)),
                    backgroundColor = bg,
                    surfaceColor = Color(0xFF1A1A20),
                    onSurfaceColor = Color(0xFFF4F4F5),
                    onSurfaceVariantColor = Color(0xFFA1A1AA),
                    accentColor = Color(0xFFD4AF37),
                    borderColor = Color(0xFF33333C),
                    cardShape = RoundedCornerShape(12.dp),
                    buttonShape = RoundedCornerShape(10.dp),
                    chipShape = RoundedCornerShape(8.dp),
                    borderWidth = 1.dp,
                    titleFontWeight = FontWeight.SemiBold,
                    bodyFontFamily = FontFamily.Serif,
                    isDarkBackground = true,
                    heroPaddingVertical = 48.dp,
                    badgeStyleLabel = "PREMIUM LUXURY"
                )
            }
            StorefrontTheme.CREATIVE -> {
                val primary = parseColor(shop.primaryColor, Color(0xFFF43F5E)) // Rose Coral
                val bg = parseColor(shop.backgroundColor, Color(0xFFFFFBEB)) // Warm Vanilla Cream
                StorefrontThemeTokens(
                    theme = theme,
                    primaryColor = primary,
                    onPrimaryColor = Color.White,
                    secondaryColor = parseColor(shop.secondaryColor, Color(0xFFF59E0B)),
                    backgroundColor = bg,
                    surfaceColor = Color.White,
                    onSurfaceColor = Color(0xFF1C1917),
                    onSurfaceVariantColor = Color(0xFF78716C),
                    accentColor = Color(0xFF8B5CF6),
                    borderColor = Color(0xFFFDE68A),
                    cardShape = RoundedCornerShape(20.dp),
                    buttonShape = RoundedCornerShape(18.dp),
                    chipShape = RoundedCornerShape(16.dp),
                    borderWidth = 1.5.dp,
                    titleFontWeight = FontWeight.ExtraBold,
                    bodyFontFamily = FontFamily.SansSerif,
                    isDarkBackground = false,
                    heroPaddingVertical = 40.dp,
                    badgeStyleLabel = "EXPRESSIVE CREATIVE"
                )
            }
        }
    }
}
