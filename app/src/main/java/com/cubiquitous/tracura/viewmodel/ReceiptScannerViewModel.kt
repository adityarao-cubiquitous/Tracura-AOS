package com.cubiquitous.tracura.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cubiquitous.tracura.model.DepartmentLineItemData
import com.cubiquitous.tracura.model.MatchedLineItemResult
import com.cubiquitous.tracura.model.ReceiptResult
import com.cubiquitous.tracura.service.ReceiptAnalysisService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptScannerViewModel @Inject constructor() : ViewModel() {

    sealed class ScannerUiState {
        object Idle : ScannerUiState()
        object Scanning : ScannerUiState()
        data class Success(val result: ReceiptResult) : ScannerUiState()
        /** Emitted after analyzeReceiptForLineItems() completes. */
        data class LineItemsMatched(val matches: List<MatchedLineItemResult>) : ScannerUiState()
        data class Error(val message: String) : ScannerUiState()
    }

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /** 0.0 → idle/starting, 0.3 → OCR done, 0.8 → matching done, 1.0 → complete */
    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()

    private val analysisService = ReceiptAnalysisService()

    // ------------------------------------------------------------------
    // Standard single-field extraction (existing flow)
    // ------------------------------------------------------------------

    fun analyzeReceipt(
        bitmap: Bitmap,
        candidateCategories: List<String> = emptyList(),
        candidateItems: List<String> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ScannerUiState.Scanning
            _analysisProgress.value = 0f
            try {
                val result = analysisService.analyze(bitmap, candidateCategories, candidateItems)
                _uiState.value = ScannerUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.message ?: "Failed to scan receipt")
            } finally {
                _analysisProgress.value = 0f
            }
        }
    }

    // ------------------------------------------------------------------
    // Line-item matching flow (mirrors Swift analyzeReceipt with departmentLineItems)
    // ------------------------------------------------------------------

    fun analyzeReceiptForLineItems(
        bitmap: Bitmap,
        availableLineItems: List<DepartmentLineItemData>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = ScannerUiState.Scanning
            _analysisProgress.value = 0f
            try {
                val matches = analysisService.analyzeReceiptForLineItems(
                    bitmap          = bitmap,
                    availableLineItems = availableLineItems,
                    onProgress      = { p -> _analysisProgress.value = p }
                )
                _analysisProgress.value = 1f
                _uiState.value = ScannerUiState.LineItemsMatched(matches)
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(e.message ?: "Failed to analyze receipt")
            } finally {
                _analysisProgress.value = 0f
            }
        }
    }

    fun reset() {
        _uiState.value = ScannerUiState.Idle
        _analysisProgress.value = 0f
    }
}
