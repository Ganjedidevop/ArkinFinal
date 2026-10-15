package com.example.arkin.data.ai

/**
 * Backward compatibility forwarder. All AI operations are now powered by [GroqHelper]
 * using Groq's high-speed OpenAI-compatible API.
 */
@Deprecated("Use GroqHelper instead", ReplaceWith("GroqHelper"))
object GeminiHelper {

    suspend fun askAssistant(
        userPrompt: String,
        contextNotes: String = "",
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): String {
        return GroqHelper.askAssistant(userPrompt, contextNotes, conversationHistory)
    }

    suspend fun solveAssignmentQuestion(
        assignmentTitle: String,
        question: String,
        style: String = "Simple & Structured"
    ): String {
        return GroqHelper.solveAssignmentQuestion(assignmentTitle, question, style)
    }

    fun generateSmartStudentResponse(prompt: String, notesContext: String): String {
        return GroqHelper.generateSmartStudentResponse(prompt, notesContext)
    }
}
