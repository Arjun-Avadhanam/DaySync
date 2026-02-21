package com.daysync.app.feature.media.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.daysync.app.core.ui.ErrorMessage
import com.daysync.app.core.ui.LoadingIndicator
import com.daysync.app.feature.media.domain.MediaItem

@Composable
fun MediaScreen(
    modifier: Modifier = Modifier,
    viewModel: MediaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()

    // Handle back from sub-screens
    BackHandler(enabled = screenState !is MediaScreenState.List) {
        viewModel.navigateBack()
    }

    when (val state = screenState) {
        is MediaScreenState.List -> {
            when (val ui = uiState) {
                is MediaUiState.Loading -> LoadingIndicator(modifier = modifier)
                is MediaUiState.Error -> ErrorMessage(message = ui.message, modifier = modifier)
                is MediaUiState.Success -> {
                    var bottomSheetItem by remember { mutableStateOf<MediaItem?>(null) }

                    MediaListScreen(
                        items = ui.items,
                        typeFilter = ui.typeFilter,
                        statusFilter = ui.statusFilter,
                        searchQuery = ui.searchQuery,
                        viewMode = ui.viewMode,
                        counts = ui.counts,
                        onTypeFilterChange = viewModel::setTypeFilter,
                        onStatusFilterChange = viewModel::setStatusFilter,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onToggleViewMode = viewModel::toggleViewMode,
                        onItemClick = { viewModel.navigateTo(MediaScreenState.Detail(it)) },
                        onItemLongClick = { bottomSheetItem = it },
                        onAddClick = { viewModel.navigateTo(MediaScreenState.AddEdit()) },
                        modifier = modifier,
                    )

                    bottomSheetItem?.let { item ->
                        MediaStatusBottomSheet(
                            item = item,
                            onStatusChange = { status ->
                                viewModel.quickUpdateStatus(item, status)
                                bottomSheetItem = bottomSheetItem?.copy(status = status)
                            },
                            onScoreChange = { score ->
                                viewModel.quickUpdateScore(item, score)
                                bottomSheetItem = bottomSheetItem?.copy(score = score)
                            },
                            onDismiss = { bottomSheetItem = null },
                        )
                    }
                }
            }
        }

        is MediaScreenState.Detail -> {
            var detailItem by remember { mutableStateOf<MediaItem?>(null) }

            LaunchedEffect(state.itemId) {
                detailItem = viewModel.getItemById(state.itemId)
            }

            detailItem?.let { item ->
                MediaDetailScreen(
                    item = item,
                    onBack = viewModel::navigateBack,
                    onEdit = { viewModel.navigateTo(MediaScreenState.AddEdit(item.id)) },
                    onDelete = { viewModel.deleteItem(item.id) },
                    onStatusChange = { status ->
                        viewModel.quickUpdateStatus(item, status)
                        detailItem = item.copy(status = status)
                    },
                    onScoreChange = { score ->
                        viewModel.quickUpdateScore(item, score)
                        detailItem = item.copy(score = score)
                    },
                    modifier = modifier,
                )
            } ?: LoadingIndicator(modifier = modifier)
        }

        is MediaScreenState.AddEdit -> {
            MediaAddEditScreen(
                viewModel = viewModel,
                editItemId = state.itemId,
                onBack = viewModel::navigateBack,
                modifier = modifier,
            )
        }
    }
}
