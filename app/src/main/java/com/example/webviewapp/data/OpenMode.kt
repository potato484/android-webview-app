package com.example.webviewapp.data

enum class OpenMode {
    CUSTOM_TABS,
    EXTERNAL_BROWSER,
    IN_APP_WEBVIEW;

    companion object {
        fun fromString(s: String?): OpenMode? = s?.let { entries.find { e -> e.name == it } }
    }
}
