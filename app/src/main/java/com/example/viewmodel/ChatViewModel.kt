package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val chatHistory = mutableListOf<Content>()
    private var documentContext: String = ""

    init {
        // Add a welcome message
        _messages.value = listOf(
            ChatMessage("Hello! I'm your AI Document Assistant. How can I help you format, summarize, or analyze your scanned documents today?", isUser = false)
        )
    }

    fun setDocumentContext(text: String) {
        if (text.isNotBlank() && text != documentContext) {
            documentContext = text
            // Optional: reset chat history if context changes significantly?
            // Let's just update the context and maybe add a system message implicitly.
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(ChatMessage("Document context updated.", isUser = false))
            _messages.value = currentMessages
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // 1. Add user message to UI
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(ChatMessage(userText, isUser = true))
        
        // 2. Add loading indicator message for AI
        currentMessages.add(ChatMessage("", isUser = false, isLoading = true))
        _messages.value = currentMessages

        // 3. Add to API history
        chatHistory.add(Content(parts = listOf(Part(text = userText)), role = "user"))

        viewModelScope.launch {
            try {
                val systemPrompt = buildString {
                    append("You are an AI assistant for a document scanning app called 'AI Image To Text Scanner-ORC'. ")
                    append("Help users process text, translate, correct grammar, and extract insights from their scanned documents. Keep responses concise and helpful. ")
                    if (documentContext.isNotBlank()) {
                        append("Here is the current document text for context:\n")
                        append(documentContext)
                        append("\n\nAnswer the user's questions based on this document context when relevant.")
                    }
                }
                val systemInstruction = Content(
                    parts = listOf(Part(text = systemPrompt)),
                    role = "system"
                )

                val request = GenerateContentRequest(
                    contents = chatHistory.toList(),
                    systemInstruction = systemInstruction
                )

                val response = RetrofitClient.service.generateContent(BuildConfig.GEMINI_API_KEY, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I couldn't process that. Please try again."

                // 4. Update API history
                chatHistory.add(Content(parts = listOf(Part(text = responseText)), role = "model"))

                // 5. Update UI (replace loading message with actual response)
                val updatedMessages = _messages.value.toMutableList()
                if (updatedMessages.isNotEmpty() && updatedMessages.last().isLoading) {
                    updatedMessages.removeLast()
                }
                updatedMessages.add(ChatMessage(responseText, isUser = false, isLoading = false))
                _messages.value = updatedMessages

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Chat API Error", e)
                val updatedMessages = _messages.value.toMutableList()
                if (updatedMessages.isNotEmpty() && updatedMessages.last().isLoading) {
                    updatedMessages.removeLast()
                }
                updatedMessages.add(ChatMessage("Sorry, there was an error processing your request. Check your connection.", isUser = false))
                _messages.value = updatedMessages
            }
        }
    }
}
