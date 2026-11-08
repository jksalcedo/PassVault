package com.jksalcedo.passvault.data.model

/**
 * Represents the result of an export operation.
 * @param serializedData The serialized data (JSON or CSV).
 * @param successCount The number of entries successfully exported.
 * @param failedEntries The titles of entries that failed to export.
 * @param totalCount The total number of entries attempted.
 */
data class ExportResult(
    val serializedData: String,
    val successCount: Int,
    val failedEntries: List<String>,
    val totalCount: Int
) {
    val hasFailures: Boolean
        get() = failedEntries.isNotEmpty()
    
    val allSucceeded: Boolean
        get() = successCount == totalCount && failedEntries.isEmpty()
}
