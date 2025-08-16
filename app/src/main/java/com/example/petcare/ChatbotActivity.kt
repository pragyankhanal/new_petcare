package com.example.petcare

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.example.petcare.data.ChatMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class ChatbotActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var inputLayout: LinearLayout
    private val chatMessages = mutableListOf<ChatMessage>()

    private lateinit var firestore: FirebaseFirestore
    private var loggedInUserId: String? = null

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-pro-latest",
            apiKey = BuildConfig.API_KEY
        )
    }

    private val chat by lazy { generativeModel.startChat() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        firestore = FirebaseFirestore.getInstance()
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        loggedInUserId = prefs.getString("LOGGED_IN_USER_ID", null)

        if (loggedInUserId == null) {
            Toast.makeText(this, "No user logged in!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        recyclerView = findViewById(R.id.recyclerViewChat)
        messageEditText = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        inputLayout = findViewById(R.id.inputLayout)

        ViewCompat.setOnApplyWindowInsetsListener(inputLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        chatAdapter = ChatAdapter(chatMessages)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        recyclerView.adapter = chatAdapter

        sendButton.setOnClickListener {
            sendMessage()
        }

        loadChatHistory()
    }

    private fun loadChatHistory() {
        if (loggedInUserId == null) return

        firestore.collection("users")
            .document(loggedInUserId!!)
            .collection("chat_history")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { querySnapshot ->
                chatMessages.clear()
                for (document in querySnapshot.documents) {
                    // Safely get the text and isUserMessage fields
                    val text = document.getString("text") ?: ""
                    val isUserMessage = document.getBoolean("isUserMessage") ?: false

                    chatMessages.add(ChatMessage(text, isUserMessage))
                }
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(chatMessages.size - 1)
            }
            .addOnFailureListener { e ->
                Log.e("ChatbotActivity", "Error loading chat history: ${e.message}", e)
                Toast.makeText(this, "Failed to load chat history.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveMessageToFirestore(message: ChatMessage) {
        if (loggedInUserId == null) return

        val messageData = hashMapOf(
            "text" to message.text,
            "isUserMessage" to message.isUserMessage,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(loggedInUserId!!)
            .collection("chat_history")
            .add(messageData)
            .addOnFailureListener { e ->
                Log.e("ChatbotActivity", "Error saving message: ${e.message}", e)
            }
    }

    private fun sendMessage() {
        val userMessage = messageEditText.text.toString().trim()
        if (userMessage.isEmpty()) {
            return
        }

        // Add user message to local list and save to Firestore
        val userChat = ChatMessage(userMessage, true)
        chatMessages.add(userChat)
        saveMessageToFirestore(userChat)

        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        messageEditText.text.clear()
        recyclerView.scrollToPosition(chatMessages.size - 1)

        messageEditText.isEnabled = false
        sendButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val prewrittenResponse = getPrewrittenResponse(userMessage)
                delay(2000)

                val aiResponse: ChatMessage = if (prewrittenResponse != null) {
                    ChatMessage(prewrittenResponse, false)
                } else {
                    val response = chat.sendMessage(userMessage)
                    val aiMessage = response.text ?: "I'm sorry, I couldn't process that. Please try again."
                    ChatMessage(aiMessage, false)
                }

                chatMessages.add(aiResponse)
                saveMessageToFirestore(aiResponse)

                chatAdapter.notifyItemInserted(chatMessages.size - 1)
                recyclerView.scrollToPosition(chatMessages.size - 1)

            } catch (e: Exception) {
                Log.e("ChatbotActivity", "Error calling Gemini API: ${e.message}", e)
                val errorMessage = ChatMessage("Error: Could not connect to the chatbot.", false)
                chatMessages.add(errorMessage)
                saveMessageToFirestore(errorMessage)
                chatAdapter.notifyItemInserted(chatMessages.size - 1)
            } finally {
                messageEditText.isEnabled = true
                sendButton.isEnabled = true
            }
        }
    }

    private fun getPrewrittenResponse(userMessage: String): String? {
        val messageLower = userMessage.toLowerCase()

        return when {
            messageLower.contains("dog") && messageLower.contains("sick") -> "I am so sorry to hear your dog is sick! Please contact a professional veterinarian immediately. You can book an appointment with a veterinarian through the 'Book vet appointment' button on the homescree."
            messageLower.contains("caregiver") -> "Finding a caregiver is a great idea. You can use our app to search for experienced pet caregivers in your local area."
            messageLower.contains("dog") -> "Dogs are wonderful companions! They require regular walks, a balanced diet, and lots of love. Be sure to schedule your dog's annual check-up with a vet."
            messageLower.contains("cat") -> "Cats are independent and playful pets. Ensure your cat has a clean litter box, access to fresh water, and engaging toys to keep them happy and healthy."
            else -> null
        }
    }
}
