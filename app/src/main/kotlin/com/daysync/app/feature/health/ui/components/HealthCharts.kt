package com.daysync.app.feature.health.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.health.model.HeartRateTrendPoint
import com.daysync.app.feature.health.model.SleepTrendPoint
import com.daysync.app.feature.health.model.StepsTrendPoint
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
import kotlinx.coroutines.runBlocking

@Composable
fun StepsTrendChart(
    data: List<StepsTrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    ChartCard(title = "Steps Trend", modifier = modifier) {
        val modelProducer = remember(data) {
            CartesianChartModelProducer().apply {
                runBlocking {
                    runTransaction {
                        columnSeries { series(data.map { it.steps }) }
                    }
                }
            }
        }
        val labelFormatter = remember(data) {
            CartesianValueFormatter { _, x, _ ->
                data.getOrNull(x.toInt())?.label ?: ""
            }
        }

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

@Composable
fun HeartRateTrendChart(
    data: List<HeartRateTrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    ChartCard(title = "Heart Rate Trend", modifier = modifier) {
        val modelProducer = remember(data) {
            CartesianChartModelProducer().apply {
                runBlocking {
                    runTransaction {
                        lineSeries {
                            series(data.map { it.avg })
                            series(data.map { it.max })
                            series(data.map { it.min })
                        }
                    }
                }
            }
        }
        val labelFormatter = remember(data) {
            CartesianValueFormatter { _, x, _ ->
                data.getOrNull(x.toInt())?.label ?: ""
            }
        }

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(
                        LineCartesianLayer.rememberLine(fill = remember { LineCartesianLayer.LineFill.single(fill(Color(0xFFE53935))) }),
                        LineCartesianLayer.rememberLine(fill = remember { LineCartesianLayer.LineFill.single(fill(Color(0xFFEF9A9A))) }),
                        LineCartesianLayer.rememberLine(fill = remember { LineCartesianLayer.LineFill.single(fill(Color(0xFFFFCDD2))) }),
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

@Composable
fun SleepTrendChart(
    data: List<SleepTrendPoint>,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    ChartCard(title = "Sleep Trend", modifier = modifier) {
        val modelProducer = remember(data) {
            CartesianChartModelProducer().apply {
                runBlocking {
                    runTransaction {
                        columnSeries {
                            series(data.map { it.totalMinutes / 60.0 })
                        }
                    }
                }
            }
        }
        val labelFormatter = remember(data) {
            CartesianValueFormatter { _, x, _ ->
                data.getOrNull(x.toInt())?.label ?: ""
            }
        }

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
