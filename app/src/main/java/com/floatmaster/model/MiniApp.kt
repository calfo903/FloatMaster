package com.floatmaster.model

data class MiniApp(
    val type: WindowType,
    val title: String = type.title,
    val description: String = type.description,
    val isFavorite: Boolean = false
)

object MiniAppCatalog {
    val all = listOf(
        MiniApp(WindowType.AI_GROUP, title = "AI Chats (12)", description = "10+ floating AI pods"),
        MiniApp(WindowType.BROWSER),
        MiniApp(WindowType.NOTES),
        MiniApp(WindowType.CALCULATOR),
        MiniApp(WindowType.DOCUMENT),
        MiniApp(WindowType.FILE_MANAGER),
        MiniApp(WindowType.CLIPBOARD),
        MiniApp(WindowType.CLOCK),
        MiniApp(WindowType.YOUTUBE),
        MiniApp(WindowType.TRANSLATOR),
        MiniApp(WindowType.MUSIC),
        MiniApp(WindowType.QUICK_SETTINGS),
        MiniApp(WindowType.APP_LAUNCHER, title = "All Apps"),
        MiniApp(WindowType.URL_WINDOW, title = "New URL Window"),
        MiniApp(WindowType.WIDGET, title = "Widget")
    )

    /** AI singles shown inside AI Group, not in main grid to avoid clutter — expose separately if needed */
    val aiSingles = WindowType.aiSingles.map { MiniApp(it) }
}
