package com.example.webviewapp.util

import android.content.Context
import android.content.SharedPreferences
import com.example.webviewapp.data.OpenMode

class AppPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    var defaultOpenMode: OpenMode
        get() = OpenMode.fromString(prefs.getString(KEY_DEFAULT_OPEN_MODE, null)) ?: OpenMode.CUSTOM_TABS
        set(value) = prefs.edit().putString(KEY_DEFAULT_OPEN_MODE, value.name).apply()

    /**
     * Global preferred browser package name for ACTION_VIEW / Custom Tabs.
     * `null` means use system default.
     */
    var preferredBrowserPackage: String?
        get() = prefs.getString(KEY_PREFERRED_BROWSER_PACKAGE, null)?.takeIf { it.isNotBlank() }
        set(value) = prefs.edit().putString(KEY_PREFERRED_BROWSER_PACKAGE, value).apply()

    var confirmHttpEveryTime: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_HTTP, true)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_HTTP, value).apply()

    var didMigrateLegacyUrl: Boolean
        get() = prefs.getBoolean(KEY_MIGRATED, false)
        set(value) = prefs.edit().putBoolean(KEY_MIGRATED, value).apply()

    companion object {
        private const val KEY_DEFAULT_OPEN_MODE = "default_open_mode"
        private const val KEY_PREFERRED_BROWSER_PACKAGE = "preferred_browser_package"
        private const val KEY_CONFIRM_HTTP = "confirm_http_every_time"
        private const val KEY_MIGRATED = "did_migrate_legacy_url"
    }
}
