package com.floatmaster.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class WindowType(
    val title: String,
    val icon: ImageVector,
    val defaultWidthDp: Int = 340,
    val defaultHeightDp: Int = 480,
    val resizable: Boolean = true,
    val description: String
) {
    BROWSER("Browser", Icons.Default.Language, 380, 560, true, "Floating web browser"),
    NOTES("Notes", Icons.Default.Note, 340, 480, true, "Rich notes"),
    CALCULATOR("Calculator", Icons.Default.Calculate, 300, 440, false, "Floating calculator"),
    DOCUMENT("Documents", Icons.Default.PictureAsPdf, 380, 520, true, "PDF / DOC viewer"),
    FILE_MANAGER("Files", Icons.Default.Folder, 360, 520, true, "File manager"),
    CLIPBOARD("Clipboard", Icons.Default.ContentPaste, 340, 400, true, "Clipboard history"),
    CLOCK("Clock", Icons.Default.Schedule, 280, 340, false, "Clock / Timer / Stopwatch"),
    YOUTUBE("YouTube", Icons.Default.PlayCircle, 380, 320, true, "Floating YouTube"),
    TRANSLATOR("Translator", Icons.Default.Translate, 360, 480, true, "Translator"),
    MUSIC("Music", Icons.Default.MusicNote, 340, 240, true, "Music controls"),
    QUICK_SETTINGS("Quick Settings", Icons.Default.Settings, 320, 380, false, "Brightness / Volume / Toggles"),
    APP_LAUNCHER("Apps", Icons.Default.Apps, 360, 500, true, "Launch any app floating"),
    URL_WINDOW("URL", Icons.Default.Link, 380, 560, true, "Custom URL window"),
    WIDGET("Widget", Icons.Default.Widgets, 320, 320, true, "Home screen widget"),

    // --- AI Chat Group (10+ providers) ---
    AI_GROUP("AI Chats", Icons.Default.SmartToy, 420, 640, true, "10+ AI chats in group · tiled & tabbed"),
    AI_CHATGPT("ChatGPT", Icons.Default.Chat, 380, 600, true, "OpenAI ChatGPT"),
    AI_CLAUDE("Claude", Icons.Default.AutoAwesome, 380, 600, true, "Anthropic Claude"),
    AI_GEMINI("Gemini", Icons.Default.BubbleChart, 380, 600, true, "Google Gemini"),
    AI_PERPLEXITY("Perplexity", Icons.Default.Search, 380, 600, true, "Perplexity AI"),
    AI_GROK("Grok", Icons.Default.Rocket, 380, 600, true, "xAI Grok"),
    AI_DEEPSEEK("DeepSeek", Icons.Default.Memory, 380, 600, true, "DeepSeek Chat"),
    AI_COPILOT("Copilot", Icons.Default.Lightbulb, 380, 600, true, "Microsoft Copilot"),
    AI_META("Meta AI", Icons.Default.Face, 380, 600, true, "Meta AI"),
    AI_POE("Poe", Icons.Default.Forum, 380, 600, true, "Poe by Quora"),
    AI_YOU("You.com", Icons.Default.Explore, 380, 600, true, "You.com AI"),
    AI_MISTRAL("Mistral", Icons.Default.Air, 380, 600, true, "Mistral AI"),
    AI_CHARACTER("Character.AI", Icons.Default.Person, 380, 600, true, "Character.AI");

    companion object {
        fun fromString(name: String): WindowType? = entries.find { it.name == name }
        val aiSingles = listOf(
            AI_CHATGPT, AI_CLAUDE, AI_GEMINI, AI_PERPLEXITY, AI_GROK, AI_DEEPSEEK,
            AI_COPILOT, AI_META, AI_POE, AI_YOU, AI_MISTRAL, AI_CHARACTER
        )
        val aiCount: Int get() = aiSingles.size
    }
}
