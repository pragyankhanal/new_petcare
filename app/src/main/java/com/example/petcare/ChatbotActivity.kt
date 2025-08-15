package com.example.petcare

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.example.petcare.data.ChatMessage
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = BuildConfig.API_KEY
        )
    }

    // Create a new chat session to manage context
    private val chat by lazy { generativeModel.startChat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        recyclerView = findViewById(R.id.recyclerViewChat)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)

        chatAdapter = ChatAdapter(chatMessages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Messages will be added to the bottom
        }
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val userMessage = messageEditText.text.toString().trim()
        if (userMessage.isEmpty()) {
            return
        }

        // Add user message to the list and update UI
        chatMessages.add(ChatMessage(userMessage, true))
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        messageEditText.text.clear()
        recyclerView.scrollToPosition(chatMessages.size - 1)

        // Disable input while processing
        messageEditText.isEnabled = false
        sendButton.isEnabled = false

        // Send message to Gemini API in a coroutine
        lifecycleScope.launch {
            try {
                // Use the chat object's sendMessage method for conversational context
                val response = chat.sendMessage(userMessage)

                // Add AI response to the list and update UI
                val aiMessage = response.text ?: "I'm sorry, I couldn't process that. Please try again."
                chatMessages.add(ChatMessage(aiMessage, false))
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                recyclerView.scrollToPosition(chatMessages.size - 1)
            } catch (e: Exception) {
                Log.e("ChatbotActivity", "Error calling Gemini API: ${e.message}", e)
                chatMessages.add(ChatMessage("Error: Could not connect to the chatbot.", false))
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
            } finally {
                // Re-enable input
                messageEditText.isEnabled = true
                sendButton.isEnabled = true
            }
        }
    }
}