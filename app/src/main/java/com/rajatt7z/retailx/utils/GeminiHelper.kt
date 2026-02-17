package com.rajatt7z.retailx.utils

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.rajatt7z.retailx.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val generativeModel: GenerativeModel

    init {
        val apiKey = BuildConfig.API_KEY
        val modelName = "gemini-3-flash-preview"
        
        if (apiKey.isBlank()) {
            throw IllegalStateException("Missing API Key")
        }
        
        generativeModel = GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

    fun generateProductDescription(name: String, category: String): Flow<String> = flow {
        val prompt = "Generate a catchy, professional product description for a '$category' product named '$name'. Keep it under 2 sentences. Focus on benefits."
        try {
            val response = generativeModel.generateContent(prompt)
            emit(response.text ?: "No description generated.")
        } catch (e: Exception) {
            emit("Error generating description: ${e.message}")
        }
    }

    fun provideReorderSuggestions(inventoryData: String): Flow<String> = flow {
        val prompt = """
            Analyze the following inventory data and suggest which items need reordering.
            Consider low stock levels (less than 10 is critical).
            
            Inventory Data:
            $inventoryData
            
            Output format:
            - List top 3 items to reorder.
            - Provide a brief 1-sentence reason for each based on stock level.
            - If everything is well stocked, say "Inventory looks healthy."
        """.trimIndent()
        
        try {
            val response = generativeModel.generateContent(prompt)
            emit(response.text ?: "Unable to analyze inventory.")
        } catch (e: Exception) {
            emit("Error analyzing inventory: ${e.message}")
        }
    }
    
    // For Chatbot
    fun chatWithBot(history: List<com.google.ai.client.generativeai.type.Content>, userMessage: String): Flow<String> = flow {
        val chat = generativeModel.startChat(history)
        
        try {
            val response = chat.sendMessage(userMessage)
            emit(response.text ?: "I didn't understand that.")
        } catch (e: Exception) {
             emit("Error: ${e.message}")
        }
    }
}
