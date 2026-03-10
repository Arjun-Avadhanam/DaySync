package com.daysync.app.feature.sync.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daysync.app.core.database.dao.SyncLogDao
import com.daysync.app.core.sync.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncEngine: SyncEngine,
    private val syncLogDao: SyncLogDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                syncEngine.syncState,
                syncLogDao.observeAll(),
                syncLogDao.observeLastSync(),
            ) { syncState, logs, lastLog ->
                SyncUiState(
                    syncState = syncState,
                    recentLogs = logs.take(30),
                    lastSyncLog = lastLog,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            syncEngine.syncAll()
        }
    }
}
