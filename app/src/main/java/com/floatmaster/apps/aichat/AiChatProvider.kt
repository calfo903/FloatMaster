package com.floatmaster.apps.aichat

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.floatmaster.model.WindowType

/**
 * Canonical AI providers. The host is the security boundary; paths may change without widening trust.
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
    val sendSelectors: List<String> = listOf("button[type=submit]", "button[aria-label*=\"Send\"]")
) {
    CHATGPT("ChatGPT", "https://chatgpt.com/", WindowType.AI_CHATGPT, Icons.Default.Chat, "#10A37F", "gpt", "OpenAI — GPT chat", sendSelectors = listOf("button[data-testid=\"send-button\"]", "button[type=submit]")),
    CLAUDE("Claude", "https://claude.ai/", WindowType.AI_CLAUDE, Icons.Default.AutoAwesome, "#6B4B8A", "claude", "Anthropic — Claude", inputSelector = "div[contenteditable=true][data-placeholder], div.ProseMirror[contenteditable=true]", sendSelectors = listOf("button[aria-label=\"Send message\"]", "button[type=submit]")),
    GEMINI("Gemini", "https://gemini.google.com/", WindowType.AI_GEMINI, Icons.Default.BubbleChart, "#4285F4", "gemini", "Google — Gemini", inputSelector = "rich-textarea[aria-label], div[contenteditable=true][aria-label=\"Ask Gemini\"]", sendSelectors = listOf("button[aria-label=\"Send message\"]", "button[type=submit]")),
    PERPLEXITY("Perplexity", "https://www.perplexity.ai/", WindowType.AI_PERPLEXITY, Icons.Default.Search, "#20808D", "pplx", "Perplexity — search-grounded", inputSelector = "textarea[placeholder*=\"Ask\"], textarea[placeholder*=\"Follow\"]", sendSelectors = listOf("button[aria-label=\"Submit\"]", "button[type=submit]")),
    GROK("Grok", "https://grok.com/", WindowType.AI_GROK, Icons.Default.Rocket, "#000000", "grok", "xAI — Grok", inputSelector = "textarea[placeholder], div[contenteditable=true]", sendSelectors = listOf("button[type=submit]", "button[aria-label=\"Send\"]")),
    DEEPSEEK("DeepSeek", "https://chat.deepseek.com/", WindowType.AI_DEEPSEEK, Icons.Default.Memory, "#4D6BFE", "deepseek", "DeepSeek — reasoning", inputSelector = "textarea[placeholder*=\"Message\"], textarea[id*=\"chat-input\"]", sendSelectors = listOf("button[type=submit]", "div[role=\"button\"]")),
    COPILOT("Copilot", "https://copilot.microsoft.com/", WindowType.AI_COPILOT, Icons.Default.Lightbulb, "#0F7BCA", "copilot", "Microsoft — Copilot", inputSelector = "textarea[placeholder*=\"Ask\"], div[contenteditable=true]", sendSelectors = listOf("button[aria-label=\"Submit\"]", "button[type=submit]")),
    META("Meta AI", "https://www.meta.ai/", WindowType.AI_META, Icons.Default.Face, "#0668E1", "meta", "Meta — AI", inputSelector = "textarea, div[contenteditable=true][role=\"textbox\"]", sendSelectors = listOf("button[type=submit]", "button[aria-label=\"Send\"]")),
    POE("Poe", "https://poe.com/", WindowType.AI_POE, Icons.Default.Forum, "#5A2D82", "poe", "Poe — multi-bot chat", inputSelector = "textarea[placeholder], div[contenteditable=true]", sendSelectors = listOf("button[type=submit]", "button[aria-label*=\"Send\"]")),
    YOU("You.com", "https://you.com/", WindowType.AI_YOU, Icons.Default.Explore, "#000000", "you", "You.com — search + chat", inputSelector = "textarea, input[type=text][placeholder*=\"Ask\"]", sendSelectors = listOf("button[type=submit]", "button[aria-label=\"Send\"]")),
    MISTRAL("Mistral", "https://chat.mistral.ai/chat", WindowType.AI_MISTRAL, Icons.Default.Air, "#FF7000", "mistral", "Mistral — Le Chat", inputSelector = "textarea[placeholder*=\"Ask\"], textarea", sendSelectors = listOf("button[type=submit]", "button[aria-label*=\"Send\"]")),
    CHARACTER("Character.AI", "https://character.ai/", WindowType.AI_CHARACTER, Icons.Default.Person, "#121212", "char", "Character.AI — roleplay bots", inputSelector = "textarea[placeholder*=\"Type\"], input[placeholder*=\"Message\"]", sendSelectors = listOf("button[type=submit]", "button[aria-label*=\"Send\"]"));

    val host: String
        get() = Uri.parse(url).host.orEmpty().lowercase()

    companion object {
        val all: List<AiChatProvider> = entries
        fun fromWindowType(type: WindowType): AiChatProvider? = entries.firstOrNull { it.windowType == type }

        /** WHY: Exact hostname comparison prevents evilchatgpt.com / chatgpt.com.attacker.example spoofing. */
        fun fromUrl(url: String): AiChatProvider? {
            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
            if (uri.scheme?.lowercase() != "https") return null
            val host = uri.host?.lowercase() ?: return null
            return entries.firstOrNull { it.host == host }
        }

        fun isAllowedUrl(url: String, provider: AiChatProvider): Boolean {
            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
            return uri.scheme?.lowercase() == "https" && uri.host?.lowercase() == provider.host
        }
    }
}

/** Desktop Chrome UA — many AI sites refuse default WebView UA. */
const val AI_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
const val AI_MOBILE_UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
