package com.bsdevs.uicomponents.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BabyBlue80,
    onPrimary = OnBabyBackground,
    primaryContainer = BabyBlueDark,
    onPrimaryContainer = OnBabyBackground,
    
    secondary = SoftPink80,
    onSecondary = OnBabyBackground,
    secondaryContainer = SoftPinkDark,
    onSecondaryContainer = OnBabyBackground,
    
    tertiary = MintGreen80,
    onTertiary = OnBabyBackground,
    tertiaryContainer = MintGreen,
    onTertiaryContainer = OnBabyBackground,
    
    background = OnBabyBackground,
    onBackground = BabyBackground,
    
    surface = OnBabyBackground,
    onSurface = BabyBackground,
    
    surfaceVariant = OnBabyBackground.copy(alpha = 0.8f),
    onSurfaceVariant = BabyBackground,

    outline = BabyBlue80
)

private val LightColorScheme = lightColorScheme(
    // 🎨 Main accent colors
    primary = BabyBlueDark,
    onPrimary = OnBabyBackground,
    primaryContainer = BabyBlue, // Soft Blue
    onPrimaryContainer = OnBabyBackground,
    
    secondary = SoftPinkDark,
    onSecondary = OnBabyBackground,
    secondaryContainer = BabyBlue, 
    onSecondaryContainer = OnBabyBackground,

    tertiary = MintGreen,
    onTertiary = OnBabyBackground,
    tertiaryContainer = SoftYellow,
    onTertiaryContainer = OnBabyBackground,

    // 🏠 Background & Surface colors
    background = BabyBackground, // Cream
    onBackground = OnBabyBackground,
    
    surface = BabySurface, // Pure White for Cards/Tiles
    onSurface = OnBabyBackground,
    
    // Used for activity feed items to give them a soft blue tint
    surfaceVariant = BabyBlue.copy(alpha = 0.15f), 
    onSurfaceVariant = OnBabyBackground,

    surfaceContainer = BabySurface,
    surfaceContainerHigh = BabySurface, 
    surfaceContainerLow = BabyBackground,

    outline = BabyBlueDark.copy(alpha = 0.5f),
    outlineVariant = BabyBlueDark.copy(alpha = 0.1f),
    
    // Bottom Bar and Rail colors
    inverseSurface = OnBabyBackground,
    inverseOnSurface = BabyBackground
)

@Composable
fun MultiModuleProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
}
