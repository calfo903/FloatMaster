package com.floatmaster.apps.aichat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.floatmaster.model.WindowType

/**
 * 12 providers — each is a floating WebView pod.
 * URLs are the canonical chat entrypoints. We use desktop User-Agent
 * so sites that block mobile WebView still render.
 */
enum class AiChatProvider(
    val displayName: String,
    val url: String,
    val windowType: WindowType,
    val icon: ImageVector,
    val brandColorHex: String,
    val shortId: String,
    val description: String,
    val inputSelector: String = "textarea, [contenteditable=true], input[type=text]",
    val sendSelectors: List<String> = listOf("button")
) {
    CHATGPT(
        "ChatGPT", "https://chatgpt.com/", WindowType.AI_CHATGPT, Icons.Default.Chat,
        "#10A37F", "gpt", "OpenAI — GPT-4o, o1, Canvas",
        inputSelector = "textarea[data-id=\"root\"], textarea#prompt-textarea, div#prompt-textarea[contenteditable=true]",
        sendSelectors = listOf("button[data-testid=\"send-button\"]", "button[data-testid=\"fruitjuice-send-button\"]", "button:has-text(\"Send\")")
    ),
    CLAUDE(
        "Claude", "https://claude.ai/", WindowType.AI_CLAUDE, Icons.Default.AutoAwesome,
        "#6B4B8A", "claude", "Anthropic — Sonnet 3.5, Artifacts",
        inputSelector = "div[contenteditable=true][data-placeholder], div.ProseMirror[contenteditable=true]",
        sendSelectors = listOf("button[aria-label=\"Send message\"]", "button:has-text(\"Send\")")
    ),
    GEMINI(
        "Gemini", "https://gemini.google.com/", WindowType.AI_GEMINI, Icons.Default.BubbleChart,
        "#4285F4", "gemini", "Google — 1.5 Pro, Live",
        inputSelector = "rich-textarea[aria-label], div[contenteditable=true][aria-label=\"Ask Gemini\"]",
        sendSelectors = listOf("button[aria-label=\"Send message\"]", "button:has-text(\"Send\")")
    ),
    PERPLEXITY(
        "Perplexity", "https://www.perplexity.ai/", WindowType.AI_PERPLEXITY, Icons.Default.Search,
        "#20808D", "pplx", "Perplexity — search-grounded",
        inputSelector = "textarea[placeholder*=\"Ask\"], textarea[placeholder*=\"Follow\"]",
        sendSelectors = listOf("button[aria-label=\"Submit\"]", "button:has-text(\"Submit\")")
    ),
    GROK(
        "Grok", "https://grok.com/", WindowType.AI_GROK, Icons.Default.Rocket,
        "#000000", "grok", "xAI — Grok 2, realtime X",
        inputSelector = "textarea[placeholder], div[contenteditable=true]",
        sendSelectors = listOf("button[type=\"submit\"]", "button:has-text(\"Send\")", "button[aria-label=\"Send\"]")
    ),
    DEEPSEEK(
        "DeepSeek", "https://chat.deepseek.com/", WindowType.AI_DEEPSEEK, Icons.Default.Memory,
        "#4D6BFE", "deepseek", "DeepSeek — V3, R1 reasoning",
        inputSelector = "textarea[placeholder*=\"Message\"], textarea[id*=\"chat-input\"]",
        sendSelectors = listOf("button:has-text(\"Send\")", "div[role=\"button\"]:has-text(\"Send\")")
    ),
    COPILOT(
        "Copilot", "https://copilot.microsoft.com/", WindowType.AI_COPILOT, Icons.Default.Lightbulb,
        "#0F7BCA", "copilot", "Microsoft — GPT-4o, Designer",
        inputSelector = "textarea[placeholder*=\"Ask\"], div[contenteditable=true]",
        sendSelectors = listOf("button[aria-label=\"Submit\"]", "button:has-text(\"Submit\")")
    ),
    META(
        "Meta AI", "https://www.meta.ai/", WindowType.AI_META, Icons.Default.Face,
        "#0668E1", "meta", "Meta — Llama 3",
        inputSelector = "textarea, div[contenteditable=true][role=\"textbox\"]",
        sendSelectors = listOf("button[type=\"submit\"]", "button[aria-label=\"Send\"]")
    ),
    POE(
        "Poe", "https://poe.com/", WindowType.AI_POE, Icons.Default.Forum,
        "#5A2D82", "poe", "Quora — 100+ bots in one",
        inputSelector = "textarea[placeholder], div[contenteditable=true]",
        sendSelectors = listOf("button:has-text(\"Send\")", "button:has-text(\"Chat\")")
    ),
    YOU(
        "You.com", "https://you.com/", WindowType.AI_YOU, Icons.Default.Explore,
        "#000000", "you", "You.com — search + chat",
        inputSelector = "textarea, input[type=text][placeholder*=\"Ask\"]",
        sendSelectors = listOf("button[type=\"submit\"]", "button[aria-label=\"Send\"]")
    ),
    MISTRAL(
        "Mistral", "https://chat.mistral.ai/chat", WindowType.AI_MISTRAL, Icons.Default.Air,
        "#FF7000", "mistral", "Mistral — Large, Le Chat",
        inputSelector = "textarea[placeholder*=\"Ask\"], textarea",
        sendSelectors = listOf("button:has-text(\"Send\")", "button[type=\"submit\"]")
    ),
    CHARACTER(
        "Character.AI", "https://character.ai/", WindowType.AI_CHARACTER, Icons.Default.Person,
        "#121212", "char", "Character.AI — roleplay bots",
        inputSelector = "textarea[placeholder*=\"Type\"], input[placeholder*=\"Message\"]",
        sendSelectors = listOf("button:has-text(\"Send\")", "button[type=\"submit\"]")
    );

    companion object {
        val all: List<AiChatProvider> = entries
        fun fromWindowType(type: WindowType): AiChatProvider? = entries.find { it.windowType == type }
        fun fromUrl(url: String): AiChatProvider? = entries.find { url.contains(it.url.substringAfter("https://").substringBefore("/")) }
    }
}

/** Desktop Chrome UA — many AI sites refuse default WebView UA */
const val AI_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
const val AI_MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
