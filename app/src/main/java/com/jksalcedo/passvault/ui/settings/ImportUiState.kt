package com.jksalcedo.passvault.ui.settings

import com.jksalcedo.passvault.data.ImportResult

/**
 * Represents the state of the import UI.
 */
sealed class ImportUiState {

    /**
     * The initial state of the import UI.
     */
    object Idle : ImportUiState()

    /**
     * The state of the import UI when it is loading.
     */
    data class Loading(val progress: Int, val total: Int) : ImportUiState()

    /**
     * The state of the import UI when the import is successful.
     * @param count The number of entries imported.
     * @param results The list of import results.
     */
    data class Success(val count: Int, val results: List<ImportResult>) : ImportUiState()

    /**
     * The state of the import UI when an error occurs.
     * @param exception The exception that occurred.
     */
    data class Error(val exception: Throwable) : ImportUiState()
}