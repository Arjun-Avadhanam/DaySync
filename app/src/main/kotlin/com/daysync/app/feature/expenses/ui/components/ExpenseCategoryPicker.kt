package com.daysync.app.feature.expenses.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.daysync.app.feature.expenses.model.ExpenseCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseCategoryPicker(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        ExpenseCategory.entries.forEach { category ->
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (category.subcategories.isEmpty()) {
                    val value = category.displayName
                    FilterChip(
                        selected = selectedCategory == value,
                        onClick = {
                            onCategorySelected(if (selectedCategory == value) null else value)
                        },
                        label = { Text(category.displayName) },
                    )
                } else {
                    category.subcategories.forEach { sub ->
                        val value = "${category.displayName} > $sub"
                        FilterChip(
                            selected = selectedCategory == value,
                            onClick = {
                                onCategorySelected(if (selectedCategory == value) null else value)
                            },
                            label = { Text(sub) },
                        )
                    }
                }
            }
        }
    }
}
