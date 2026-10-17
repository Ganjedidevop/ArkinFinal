# Groq AI Production Architecture & Firebase Cloud Functions Setup

This guide documents how to keep your Groq API key secret for production releases of **Arkin** using Firebase Cloud Functions (Task 6).

---

## 1. Why a Backend Proxy for Production?
While local builds use the Gradle Secrets plugin with `.env` (which prevents API keys from being committed to Git), releasing an Android APK/Bundle with an embedded client-side key allows attackers to decompile the APK or inspect network traffic.

Routing AI requests through a backend server or Firebase Cloud Functions ensures:
1. **The Groq API key is never distributed to client devices.**
2. **Client authentication via Firebase Auth:** Only authenticated users of your app can trigger AI requests.
3. **Usage controls and rate limiting:** Prevents abuse or unexpected billing spikes.

---

## 2. Firebase Cloud Function Implementation

Create a Cloud Function in your Firebase project (`arkin-22b82`):

### `functions/src/index.ts`
```typescript
import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import axios from "axios";

admin.initializeApp();

// Store your key in Firebase Secrets Manager:
// firebase functions:secrets:set GROQ_API_KEY
export const chatWithGroq = functions
  .runWith({ secrets: ["GROQ_API_KEY"] })
  .https.onCall(async (data, context) => {
    // 1. Authenticate user
    if (!context.auth) {
      throw new functions.https.HttpsError(
        "unauthenticated",
        "User must be authenticated with Firebase Auth to use Arkin AI."
      );
    }

    const { model = "openai/gpt-oss-20b", messages, temperature = 0.7, max_tokens = 1500 } = data;

    if (!messages || !Array.isArray(messages)) {
      throw new functions.https.HttpsError(
        "invalid-argument",
        "Messages array is required."
      );
    }

    try {
      const groqResponse = await axios.post(
        "https://api.groq.com/openai/v1/chat/completions",
        {
          model,
          messages,
          temperature,
          max_tokens,
        },
        {
          headers: {
            Authorization: `Bearer ${process.env.GROQ_API_KEY}`,
            "Content-Type": "application/json",
          },
          timeout: 30000,
        }
      );

      return groqResponse.data;
    } catch (error: any) {
      functions.logger.error("Error calling Groq API", error.response?.data || error.message);
      throw new functions.https.HttpsError(
        "internal",
        "Failed to generate AI response from Groq."
      );
    }
  });
```

---

## 3. Connecting the Android Client to Production Proxy

In `com.example.arkin.data.ai.GroqHelper`, a proxy hook is already built in:

```kotlin
// In your Application class or MainActivity during release setup:
if (!BuildConfig.DEBUG) {
    GroqHelper.backendProxyUrl = "https://us-central1-arkin-22b82.cloudfunctions.net/chatWithGroq"
}
```

When `backendProxyUrl` is set:
- `GroqHelper` routes requests directly to the backend proxy.
- The request does not include the client API key header, keeping the Groq API key 100% server-side.
