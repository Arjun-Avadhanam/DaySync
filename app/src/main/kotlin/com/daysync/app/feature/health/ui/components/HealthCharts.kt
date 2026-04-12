package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.health.model.HeartRateTrendPoint
import com.daysync.app.feature.health.model.SleepTrendPoint
import com.daysync.app.feature.health.model.StepsTrendPoint
import com.daysync.app.feature.health.model.WorkoutTrendPoint
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.component.LineComponent

@Composable
fun StepsTrendChart(
    data: List<StepsTrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    // One producer for the lifetime of this composable; data updates flow
    // through runTransaction inside a coroutine, never on the main thread.
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries { series(data.map { it.steps }) }
        }
    }
    val labelFormatter = remember(data) {
        CartesianValueFormatter { _, x, _ ->
            data.getOrNull(x.toInt())?.label ?: ""
        }
    }

    ChartCard(title = "Steps Trend", modifier = modifier) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        LineComponent(
                            fill = fill(MaterialTheme.colorScheme.primary),
                            thicknessDp = 8f,
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = labelFormatter,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    }
}

private val AvgHrColor = Color(0xFFE53935)
private val MaxHrColor = Color(0xFFEF9A9A)
private val MinHrColor = Color(0xFFFFCDD2)

@Composable
fun HeartRateTrendChart(
    data: List<HeartRateTrendPoint>,
    modifier: Modifier = Modifier,
) {
    // Line charts need ≥2 points to draw a segment (unlike column charts
    // which can render a single bar).
    if (data.size < 2) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            lineSeries {
                series(data.map { it.avg })
                series(data.map { it.max })
                series(data.map { it.min })
            }
        }
    }
    val labelFormatter = remember(data) {
        CartesianValueFormatter { _, x, _ ->
            data.getOrNull(x.toInt())?.label ?: ""
        }
    }

    ChartCard(title = "Heart Rate Trend", modifier = modifier) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(fill = remember { LineCartesianLayer.LineFill.single(fill(AvgHrColor)) }),
                        LineCartesianLayer.rememberLine(fill = remember { LineCartesianLayer.LineFill.single(fill(MaxHrColor)) }),
                        LineCartesianLayer.rememberLine(fill = remember { LineCartesianLayer.LineFill.single(fill(MinHrColor)) }),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = labelFormatter,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ChartLegendItem("Avg", AvgHrColor)
            ChartLegendItem("Max", MaxHrColor)
            ChartLegendItem("Min", MinHrColor)
        }
    }
}

@Composable
private fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
fun SleepTrendChart(
    data: List<SleepTrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(data.map { it.totalMinutes / 60.0 })
            }
        }
    }
    val labelFormatter = remember(data) {
        CartesianValueFormatter { _, x, _ ->
            data.getOrNull(x.toInt())?.label ?: ""
        }
    }

    ChartCard(title = "Sleep Trend", modifier = modifier) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        LineComponent(
                            fill = fill(Color(0xFF5C6BC0)),
                            thicknessDp = 8f,
                        ),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = labelFormatter,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    }
}

private val CaloriesColor = Color(0xFFFF7043)
private val WorkoutHrColor = Color(0xFFE53935)

@Composable
fun WorkoutTrendChart(
    data: List<WorkoutTrendPoint>,
    title: String = "Workout Trend",
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(data) {
        modelProducer.runTransaction {
            columnSeries {
                series(data.map { it.calories })
                series(data.map { it.avgHr })
            }
        }
    }
    val labelFormatter = remember(data) {
        CartesianValueFormatter { _, x, _ ->
            data.getOrNull(x.toInt())?.label ?: ""
        }
    }

    ChartCard(title = title, modifier = modifier) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        LineComponent(fill = fill(CaloriesColor), thicknessDp = 6f),
                        LineComponent(fill = fill(WorkoutHrColor), thicknessDp = 6f),
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = labelFormatter,
                    itemPlacer = HorizontalAxis.ItemPlacer.aligned(),
                ),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ChartLegendItem("Calories", CaloriesColor)
            ChartLegendItem("Avg HR", WorkoutHrColor)
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            content()
        }
    }
}
