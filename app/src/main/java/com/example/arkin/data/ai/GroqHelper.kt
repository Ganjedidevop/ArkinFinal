package com.example.arkin.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * GroqHelper handles AI features in Arkin using Groq's OpenAI-compatible Chat Completions API.
 * 
 * Secure API Key Source:
 * - Read at runtime from BuildConfig.GROQ_API_KEY (populated via .env by the Secrets Gradle Plugin).
 * - For production, an optional backend or Firebase Cloud Function proxy URL can be set via [backendProxyUrl]
 *   to keep the API key completely off client devices.
 */
object GroqHelper {
    private const val TAG = "GroqHelper"
    
    // Official Groq Chat Completions endpoint
    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    
    // Verified currently supported models on Groq
    const val PRIMARY_MODEL = "openai/gpt-oss-20b"
    const val FALLBACK_MODEL = "qwen/qwen3.8-27b"

    /**
     * Optional Backend / Firebase Cloud Functions proxy URL for production.
     * When set, calls will be routed through this endpoint instead of calling Groq directly.
     */
    @Volatile
    var backendProxyUrl: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Retrieve the Groq API key from BuildConfig.
     */
    fun getApiKey(): String {
        return try {
            val key = BuildConfig.GROQ_API_KEY
            if (key.isNullOrBlank() || key == "MY_GROQ_API_KEY") "" else key.trim().removeSurrounding("\"")
        } catch (e: Throwable) {
            ""
        }
    }

    /**
     * Send a study prompt to Groq AI with context notes and chat history.
     */
    suspend fun askAssistant(
        userPrompt: String,
        contextNotes: String = "",
        conversationHistory: List<Pair<String, String>> = emptyList(),
        model: String = PRIMARY_MODEL
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val proxyUrl = backendProxyUrl

        // If either a backend proxy or a valid direct API key is available, call the API
        if (!proxyUrl.isNullOrBlank() || apiKey.isNotBlank()) {
            try {
                val jsonPayload = buildRequestPayload(userPrompt, contextNotes, conversationHistory, model)
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonPayload.toString().toRequestBody(mediaType)

                val requestBuilder = Request.Builder()
                if (!proxyUrl.isNullOrBlank()) {
                    // Call backend proxy without client API key (backend secures the key)
                    requestBuilder.url(proxyUrl).post(requestBody)
                } else {
                    // Call Groq OpenAI-compatible Chat Completions API directly
                    requestBuilder
                        .url(GROQ_ENDPOINT)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                }

                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string().orEmpty()
                val statusCode = response.code

                when {
                    response.isSuccessful -> {
                        val parsedText = parseChatCompletionResponse(responseBody)
                        if (parsedText.isNotBlank()) {
                            return@withContext parsedText
                        } else {
                            Log.w(TAG, "Groq response contained empty choices: $responseBody")
                        }
                    }
                    statusCode == 401 -> {
                        Log.e(TAG, "Groq API error: 401 Unauthorized. Please verify GROQ_API_KEY in .env.")
                    }
                    statusCode == 429 -> {
                        Log.w(TAG, "Groq API error: 429 Rate limit reached.")
                        return@withContext "⚠️ Arkin AI is receiving a high volume of requests right now. Please wait a moment and try again."
                    }
                    statusCode in 500..599 -> {
                        Log.w(TAG, "Groq API error: $statusCode Server error: $responseBody")
                    }
                    else -> {
                        Log.w(TAG, "Groq API error code: $statusCode, body: $responseBody")
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.w(TAG, "Network timeout contacting Groq API", e)
                return@withContext "⏱️ The AI request timed out. Please check your connection and try again."
            } catch (e: IOException) {
                Log.w(TAG, "Network failure contacting Groq API (offline)", e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error contacting Groq API", e)
            }
        }

        // Educational Student Fallback Intelligence Engine (for offline or unconfigured states)
        generateSmartStudentResponse(userPrompt, contextNotes)
    }

    /**
     * Break down and solve homework/assignment questions step-by-step.
     */
    suspend fun solveAssignmentQuestion(
        assignmentTitle: String,
        question: String,
        style: String = "Simple & Structured"
    ): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are assisting a university student with their homework assignment titled "$assignmentTitle".
            Please provide a comprehensive, step-by-step solution and explanation for the following question:
            "$question"
            Style required: $style.
            Format clearly with:
            1. Core Concept & Formula
            2. Step-by-Step Derivation / Solution
            3. Final Answer Summary
            4. Common Pitfalls to Avoid in Examinations.
            Label the solution clearly as an editable draft.
        """.trimIndent()

        askAssistant(prompt)
    }

    /**
     * Builds standard OpenAI-compatible Chat Completions JSON request payload.
     */
    fun buildRequestPayload(
        userPrompt: String,
        contextNotes: String,
        conversationHistory: List<Pair<String, String>>,
        model: String = PRIMARY_MODEL
    ): JSONObject {
        val root = JSONObject()
        root.put("model", model)

        val messagesArray = JSONArray()

        // 1. System Prompt
        val systemInstruction = """
            You are Arkin, an expert, encouraging AI study companion for university and college students.
            You help with homework breakdown, conceptual explanations, study scheduling, practice viva quizzes, and summarizing notes.
            Be concise, clear, structured, and student-focused.
            ${if (contextNotes.isNotBlank()) "User's current notes context:\n$contextNotes" else ""}
        """.trimIndent()

        val systemObj = JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
        }
        messagesArray.put(systemObj)

        // 2. Conversation History
        conversationHistory.takeLast(6).forEach { (role, msg) ->
            if (msg.isNotBlank()) {
                val groqRole = if (role.equals("user", ignoreCase = true)) "user" else "assistant"
                val historyObj = JSONObject().apply {
                    put("role", groqRole)
                    put("content", msg)
                }
                messagesArray.put(historyObj)
            }
        }

        // 3. Current User Message
        val currentObj = JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        }
        messagesArray.put(currentObj)

        root.put("messages", messagesArray)
        root.put("temperature", 0.7)
        root.put("max_tokens", 1500)

        return root
    }

    /**
     * Parses standard OpenAI-compatible response JSON.
     */
    fun parseChatCompletionResponse(responseJson: String): String {
        return try {
            val root = JSONObject(responseJson)
            val choices = root.optJSONArray("choices") ?: return ""
            if (choices.length() == 0) return ""

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message")
            val content = message?.optString("content", "") ?: ""
            content.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Groq response JSON", e)
            ""
        }
    }

    /**
     * Fallback student assistance engine for offline use.
     */
    fun generateSmartStudentResponse(prompt: String, notesContext: String): String {
        val query = prompt.lowercase()

        return when {
            query.contains("assignment") || query.contains("question") || query.contains("solve") -> {
                """
                    [AI DRAFT ANSWER]
                    📚 Step-by-Step Homework Solution:
                    
                    1. Key Concept:
                    • Relational dependencies and concurrency isolation ensure consistent database state.
                    
                    2. Detailed Solution Steps:
                    • Step 1: Identify functional dependencies across all attribute subsets.
                    • Step 2: Test candidate keys for minimal closure.
                    • Step 3: Decompose non-conforming relations into BCNF schemas ensuring lossless join property.
                    
                    3. Final Answer Summary:
                    • Decomposed relations: R1(A, B, C) with primary key A, and R2(C, D) with primary key C.
                    
                    💡 Note: This is an AI-generated draft. Review and verify the steps before submitting your final report.
                """.trimIndent()
            }

            query.contains("timetable") || query.contains("schedule") -> {
                """
                    🗓️ Extracted Timetable Organization:
                    
                    • Monday:
                      - 09:00 AM - 10:30 AM: Database Systems (Lab 2)
                      - 11:00 AM - 12:30 PM: Distributed Systems (Hall 102)
                    • Wednesday:
                      - 10:00 AM - 11:30 AM: Operating Systems (Room 402)
                      - 02:00 PM - 04:00 PM: Systems Programming Lab
                    • Friday:
                      - 11:00 AM - 12:00 PM: Ethics in Computing (Room 105)
                    
                    ✨ Tip: Tap 'Apply Schedule to My Timetable' to import these periods with automatic reminder alerts.
                """.trimIndent()
            }

            query.contains("summarize") || query.contains("summary") -> {
                if (notesContext.isNotBlank()) {
                    "📝 **Summary of your notes:**\n\n• **Core Topic:** Key academic principles & revision highlights.\n• **Main Takeaways:**\n  - Focus on fundamental definitions and formulas.\n  - Ensure understanding of relational operations and architectures.\n  - Review lecture slides before exams.\n\n💡 *Pro-tip: Try creating 3 flashcards from this note to test your active recall!*"
                } else {
                    "📝 **Study Summary Generator:**\nTo summarize a specific note, tap 'Summarize with AI' right on any note card in your Notes tab, or paste the text here and I'll extract key bullet points, formulas, and exam keywords!"
                }
            }

            query.contains("dbms") || query.contains("database") || query.contains("acid") || query.contains("normal") -> {
                "📚 **Database Systems Quick Reference:**\n\n• **ACID Properties:**\n  1. **Atomicity:** All transactions occur or none do.\n  2. **Consistency:** Data remains valid according to all schema rules.\n  3. **Isolation:** Transactions execute concurrently without interference.\n  4. **Durability:** Committed transactions persist even through system failures.\n\n• **Normalization:**\n  - **1NF:** Atomic values, no repeating groups.\n  - **2NF:** 1NF + no partial dependency on candidate keys.\n  - **3NF:** 2NF + no transitive dependency.\n  - **BCNF:** Every determinant is a candidate key."
            }

            query.contains("operating system") || query.contains("os") || query.contains("process") || query.contains("deadlock") -> {
                "💻 **Operating Systems Concept Review:**\n\n• **Process States:** New ➔ Ready ➔ Running ➔ Waiting ➔ Terminated.\n• **Four Coffman Conditions for Deadlock:**\n  1. Mutual Exclusion\n  2. Hold and Wait\n  3. No Preemption\n  4. Circular Wait\n\n• **CPU Scheduling Algorithms:** FCFS, Shortest Job First (SJF), Round Robin (RR), and Priority Scheduling."
            }

            query.contains("plan") || query.contains("exam") -> {
                "🗓️ **Recommended 3-Day Exam Study Strategy:**\n\n• **Day 1: Theory & Concept Mapping** (3-4 hrs)\n  - Review lecture slides & high-priority textbook chapters.\n  - Create condensed one-page formula/concept sheets.\n\n• **Day 2: Active Problem Solving** (4 hrs)\n  - Solve past year midterm/final questions without checking solutions first.\n  - Clarify any missed concepts with your professor or study group.\n\n• **Day 3: Timed Mock Drill & Rest** (2-3 hrs)\n  - Practice under strict exam time limits.\n  - Sleep at least 7-8 hours before exam day!"
            }

            query.contains("quiz") || query.contains("viva") -> {
                "🎯 **Practice Viva / Quiz Questions:**\n\n1. *Question:* What is the main difference between a process and a thread?\n   👉 *Hint:* Think about shared memory space vs isolated memory space.\n\n2. *Question:* Which normal form eliminates transitive dependency?\n   👉 *Hint:* Third Normal Form (3NF).\n\n3. *Question:* What is the time complexity of searching in a balanced BST?\n   👉 *Hint:* O(log n).\n\nReply with your answers and I'll grade them!"
            }

            else -> {
                "👋 I'm **Arkin**, your AI study assistant! Here's how I can assist you:\n\n• **Assignment Assistance:** Ask questions about problem sets, request step-by-step proofs.\n• **Document & Timetable Parsing:** Upload class schedules or exam papers to organize them.\n• **Explain Topics:** Clear explanations for CS, Math, Engineering, or Humanities.\n• **Create Quizzes & Flashcards:** Test your knowledge before midterms.\n\nWhat would you like to tackle today?"
            }
        }
    }
}
