package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CapacityLevel
import com.example.ui.theme.CapacityHigh
import com.example.ui.theme.CapacityLow
import com.example.ui.theme.CapacityMed
import com.example.ui.theme.CapacityMutedBar

@Composable
fun CapacitySignalBars(
    capacity: CapacityLevel,
    modifier: Modifier = Modifier,
    barWidth: Dp = 4.dp,
    maxBarHeight: Dp = 14.dp,
) {
    val activeColor: Color = when (capacity) {
        CapacityLevel.HAY_LUGAR -> CapacityLow
        CapacityLevel.CASI_LLENA -> CapacityMed
        CapacityLevel.VA_LLENA -> CapacityHigh
    }
    val heights = listOf(maxBarHeight * 0.48f, maxBarHeight * 0.74f, maxBarHeight)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEachIndexed { index, barHeight ->
            val filled = index < capacity.activeBars
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(if (filled) activeColor else CapacityMutedBar)
            )
        }
    }
}
