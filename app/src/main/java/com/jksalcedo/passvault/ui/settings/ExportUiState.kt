package com.jksalcedo.passvault.ui.settings

import com.jksalcedo.passvault.data.ExportResult

/**
 * Represents the state of the export UI.
 */
sealed class ExportUiState {
    /**
     * The initial state of the export UI.
     */
    object Idle : ExportUiState()

    /**
     * The state of the export UI when it is loading.
     * @param progress The number of entries processed so far.
     * @param total The total number of entries to process.
     */
    data class Loading(val progress: Int, val total: Int) : ExportUiState()

    /**
     * The state of the export UI when the export is successful.
     * @param result The result of the export.
     */
    data class Success(val result: ExportResult) : ExportUiState()

    /**
     * The state of the export UI when an error occurs.
     * @param exception The exception that occurred.
     */
    data class Error(val exception: Throwable) : ExportUiState()
}
