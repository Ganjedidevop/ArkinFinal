package com.example.arkin.data.ai

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroqAiTest {

    @Test
    fun testBuildRequestPayload_structure() {
        val history = listOf(
            "user" to "What is a transaction?",
            "ai" to "A transaction is a unit of work in a database."
        )
        val payload = GroqHelper.buildRequestPayload(
            userPrompt = "Explain ACID properties",
            contextNotes = "DBMS Lecture 3",
            conversationHistory = history,
            model = GroqHelper.PRIMARY_MODEL
        )

        assertEquals("openai/gpt-oss-20b", payload.getString("model"))
        assertEquals(0.7, payload.getDouble("temperature"), 0.001)
        assertEquals(1500, payload.getInt("max_tokens"))

        val messages = payload.getJSONArray("messages")
        // System message + 2 history messages + 1 current prompt = 4
        assertEquals(4, messages.length())

        // 1. System message
        val sysMsg = messages.getJSONObject(0)
        assertEquals("system", sysMsg.getString("role"))
        assertTrue(sysMsg.getString("content").contains("Arkin"))
        assertTrue(sysMsg.getString("content").contains("DBMS Lecture 3"))

        // 2. History user
        val h1 = messages.getJSONObject(1)
        assertEquals("user", h1.getString("role"))
        assertEquals("What is a transaction?", h1.getString("content"))

        // 3. History assistant (mapped from "ai")
        val h2 = messages.getJSONObject(2)
        assertEquals("assistant", h2.getString("role"))
        assertEquals("A transaction is a unit of work in a database.", h2.getString("content"))

        // 4. Current user prompt
        val current = messages.getJSONObject(3)
        assertEquals("user", current.getString("role"))
        assertEquals("Explain ACID properties", current.getString("content"))
    }

    @Test
    fun testParseChatCompletionResponse_validResponse() {
        val sampleGroqJson = """
            {
              "id": "chatcmpl-test-12345",
              "object": "chat.completion",
              "created": 1729000000,
              "model": "openai/gpt-oss-20b",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "ACID stands for Atomicity, Consistency, Isolation, and Durability."
                  },
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 25,
                "completion_tokens": 12,
                "total_tokens": 37
              }
            }
        """.trimIndent()

        val parsed = GroqHelper.parseChatCompletionResponse(sampleGroqJson)
        assertEquals("ACID stands for Atomicity, Consistency, Isolation, and Durability.", parsed)
    }

    @Test
    fun testParseChatCompletionResponse_emptyChoicesOrMalformed() {
        // Empty choices
        val emptyChoicesJson = """{"choices": []}"""
        assertEquals("", GroqHelper.parseChatCompletionResponse(emptyChoicesJson))

        // No choices key
        val errorJson = """{"error": {"message": "Rate limit exceeded", "type": "rate_limit_error"}}"""
        assertEquals("", GroqHelper.parseChatCompletionResponse(errorJson))

        // Malformed JSON string
        assertEquals("", GroqHelper.parseChatCompletionResponse("Not a json response"))
    }

    @Test
    fun testSmartStudentResponse_fallbacks() {
        val assignmentFallback = GroqHelper.generateSmartStudentResponse("Help me solve this assignment question", "")
        assertTrue(assignmentFallback.contains("Homework Solution"))

        val timetableFallback = GroqHelper.generateSmartStudentResponse("Parse my timetable schedule", "")
        assertTrue(timetableFallback.contains("Timetable Organization"))

        val summaryFallback = GroqHelper.generateSmartStudentResponse("Please summarize these notes", "Operating Systems Chapter 1")
        assertTrue(summaryFallback.contains("Summary of your notes"))

        val generalFallback = GroqHelper.generateSmartStudentResponse("What can you do?", "")
        assertTrue(generalFallback.contains("Arkin"))
    }

    @Test
    fun testApiKeyRetrievedFromSecureBuildConfig() {
        val key = GroqHelper.getApiKey()
        assertNotNull("API key retrieval should not be null", key)
        assertFalse("API key should not be placeholder", key == "MY_GROQ_API_KEY")
        assertTrue("API key should start with gsk_ when configured", key.startsWith("gsk_"))
    }
}
