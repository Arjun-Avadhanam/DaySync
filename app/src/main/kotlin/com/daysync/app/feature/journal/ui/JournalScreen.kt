package com.daysync.app.feature.journal.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daysync.app.feature.journal.ui.calendar.JournalCalendarContent
import com.daysync.app.feature.journal.ui.detail.JournalDetailContent
import com.daysync.app.feature.journal.ui.editor.JournalEditorContent
import com.daysync.app.feature.journal.ui.list.JournalListContent

@Composable
fun JournalScreen(
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.screenState !is JournalScreenState.List) {
        viewModel.handleBackPress()
    }

    AnimatedContent(
        targetState = uiState.screenState,
        modifier = modifier,
        transitionSpec = {
            val forward = when {
                targetState is JournalScreenState.List -> false
                initialState is JournalScreenState.List -> true
                targetState is JournalScreenState.Editor -> true
                else -> true
            }
            if (forward) {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { it / 3 } + fadeOut())
            }
        },
        label = "JournalScreenTransition",
    ) { screenState ->
        when (screenState) {
            is JournalScreenState.List -> {
                JournalListContent(
                    entries = uiState.entries,
                    searchQuery = uiState.searchQuery,
                    isSearchActive = uiState.isSearchActive,
                    showArchived = uiState.showArchived,
                    onEntryClick = viewModel::navigateToDetail,
                    onNewEntry = { viewModel.navigateToEditor() },
                    onSearchToggle = viewModel::toggleSearch,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onCalendarClick = viewModel::navigateToCalendar,
                    onToggleArchived = viewModel::toggleShowArchived,
                )
            }

            is JournalScreenState.Calendar -> {
                JournalCalendarContent(
                    yearMonth = uiState.calendarYearMonth,
                    entries = uiState.calendarEntries,
                    onBack = { viewModel.handleBackPress() },
                    onMonthChange = viewModel::changeCalendarMonth,
                    onDayClick = { date, hasEntry ->
                        if (hasEntry) {
                            val entry = uiState.calendarEntries[date]
                            if (entry != null) viewModel.navigateToDetail(entry.id)
                        } else {
                            viewModel.navigateToEditor(prefillDate = date)
                        }
                    },
                    onEntryClick = viewModel::navigateToDetail,
                )
            }

            is JournalScreenState.Detail -> {
                JournalDetailContent(
                    entry = uiState.selectedEntry,
                    onBack = { viewModel.handleBackPress() },
                    onEdit = { viewModel.navigateToEditor(screenState.entryId) },
                    onArchive = { viewModel.toggleArchive(screenState.entryId) },
                    onDelete = { viewModel.deleteEntry(screenState.entryId) },
                )
            }

            is JournalScreenState.Editor -> {
                JournalEditorContent(
                    title = uiState.editorTitle,
                    content = uiState.editorContent,
                    mood = uiState.editorMood,
                    tags = uiState.editorTags,
                    date = uiState.editorDate,
                    availableTags = uiState.availableTags,
                    isSaving = uiState.isSaving,
                    isEditing = screenState.entryId != null,
                    isDirty = viewModel.isEditorDirty(),
                    onTitleChange = viewModel::updateEditorTitle,
                    onContentChange = viewModel::updateEditorContent,
                    onMoodChange = viewModel::updateEditorMood,
                    onTagToggle = viewModel::toggleEditorTag,
                    onCustomTagAdd = viewModel::addCustomTag,
                    onDateChange = viewModel::updateEditorDate,
                    onSave = viewModel::saveEntry,
                    onBack = { viewModel.handleBackPress() },
                )
            }
        }
    }
}
