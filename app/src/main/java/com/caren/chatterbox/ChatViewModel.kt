package com.caren.chatterbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Message(val text: String, val isUser: Boolean)

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isSending: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + Message(text = text, isUser = true),
                isSending = true
            )
        }

        // Simulate LLM response
        viewModelScope.launch {
            delay(1000)
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + Message(text = "I am a simulated LLM. You said: \"$text\"", isUser = false),
                    isSending = false
                )
            }
        }
    }
}
