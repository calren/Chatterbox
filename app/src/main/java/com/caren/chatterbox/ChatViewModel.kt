package com.caren.chatterbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Message(val text: String, val isUser: Boolean)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isSending: Boolean = false,
    val statusText: String = "Initializing..."
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val generativeModel = Generation.getClient()

    init {
        checkAndDownloadModel()
    }

    private fun checkAndDownloadModel() {
        viewModelScope.launch {
            try {
                val status = generativeModel.checkStatus()
                when (status) {
                    FeatureStatus.AVAILABLE -> {
                        _uiState.update { it.copy(statusText = "Ready") }
                    }
                    FeatureStatus.DOWNLOADABLE -> {
                        _uiState.update { it.copy(statusText = "Downloading model...") }
                        generativeModel.download().collect { }
                        _uiState.update { it.copy(statusText = "Ready") }
                    }
                    FeatureStatus.DOWNLOADING -> {
                        _uiState.update { it.copy(statusText = "Downloading model...") }
                        generativeModel.download().collect { }
                        _uiState.update { it.copy(statusText = "Ready") }
                    }
                    FeatureStatus.UNAVAILABLE -> {
                        _uiState.update { it.copy(statusText = "Model unavailable (Gemini Nano not supported)") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusText = "Error: ${e.message}") }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + Message(text = text, isUser = true),
                isSending = true
            )
        }

        viewModelScope.launch {
            try {
                val request = generateContentRequest(TextPart(text)) {}
                val response = generativeModel.generateContent(request)
                val responseText = response.candidates.firstOrNull()?.text ?: "No response text received."
                
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + Message(text = responseText, isUser = false),
                        isSending = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + Message(text = "Error generating response: ${e.message}", isUser = false),
                        isSending = false
                    )
                }
            }
        }
    }
}
