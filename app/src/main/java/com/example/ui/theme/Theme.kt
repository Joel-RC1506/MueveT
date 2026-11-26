package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MueveTLightColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = SurfaceWhite,
    primaryContainer = WarmOrangeSurface,
    onPrimaryContainer = TerracottaDark,
    secondary = ForestGreen,
    onSecondary = SurfaceWhite,
    secondaryContainer = SageTint,
    onSecondaryContainer = ForestGreen,
    tertiary = PlumTransit,
    onTertiary = SurfaceWhite,
    background = CreamBg,
    onBackground = InkPrimary,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    surfaceVariant = SageTint,
    onSurfaceVariant = InkSecondary,
    outline = CardBorder,
    outlineVariant = SoftDivider,
    error = CapacityHigh,
    onError = SurfaceWhite,
)

@Composable
fun MueveTTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MueveTLightColorScheme,
        typography = MueveTTypography,
        content = content,
    )
}
