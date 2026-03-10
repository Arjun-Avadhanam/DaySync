package com.daysync.app.feature.nutrition.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MacroBreakdownChart(
    protein: Double,
    carbs: Double,
    fat: Double,
    modifier: Modifier = Modifier,
) {
    val proteinColor = Color(0xFF4CAF50)
    val carbsColor = Color(0xFF2196F3)
    val fatColor = Color(0xFFFF9800)
    val emptyColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)

    Canvas(modifier = modifier) {
        val strokeWidth = 10.dp.toPx()
        val padding = strokeWidth / 2
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(padding, padding)

        val total = protein + carbs + fat
        if (total <= 0) {
            drawArc(
                color = emptyColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            return@Canvas
        }

        val proteinAngle = (protein / total * 360).toFloat()
        val carbsAngle = (carbs / total * 360).toFloat()
        val fatAngle = (fat / total * 360).toFloat()

        var startAngle = -90f

        if (proteinAngle > 0) {
            drawArc(
                color = proteinColor,
                startAngle = startAngle,
                sweepAngle = proteinAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            startAngle += proteinAngle + 2f
        }

        if (carbsAngle > 0) {
            drawArc(
                color = carbsColor,
                startAngle = startAngle,
                sweepAngle = carbsAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            startAngle += carbsAngle + 2f
        }

        if (fatAngle > 0) {
            drawArc(
                color = fatColor,
                startAngle = startAngle,
                sweepAngle = fatAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}
