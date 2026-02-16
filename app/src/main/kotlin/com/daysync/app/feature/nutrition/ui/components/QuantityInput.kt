package com.daysync.app.feature.nutrition.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun QuantityInput(
    amount: Double,
    onAmountChange: (Double) -> Unit,
    unitSymbol: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalIconButton(
            onClick = { if (amount > 0.5) onAmountChange(amount - 0.5) },
        ) {
            Text("-", style = MaterialTheme.typography.titleMedium)
        }

        OutlinedTextField(
            value = if (amount == amount.toLong().toDouble()) {
                amount.toLong().toString()
            } else {
                amount.toString()
            },
            onValueChange = { text ->
                text.toDoubleOrNull()?.let { if (it >= 0) onAmountChange(it) }
            },
            modifier = Modifier.width(80.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )

        Text(
            text = unitSymbol,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        FilledTonalIconButton(
            onClick = { onAmountChange(amount + 0.5) },
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}
