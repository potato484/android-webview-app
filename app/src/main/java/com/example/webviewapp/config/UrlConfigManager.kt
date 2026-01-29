package com.example.webviewapp.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import android.webkit.CookieManager
import android.webkit.WebStorage

class UrlConfigManager(context: Context) {

    companion object {
        const val DEFAULT_URL = "https://linux.do"
        private const val PREFS_NAME = "url_config"
        private const val KEY_URL = "target_url"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUrl(): String {
        val stored = prefs.getString(KEY_URL, null)
        return if (stored != null && isValidHttpsUrl(stored)) stored else DEFAULT_URL
    }

    fun saveUrl(url: String): Boolean {
        if (!isValidHttpsUrl(url)) return false
        prefs.edit().putString(KEY_URL, url).apply()
        return true
    }

    fun resetToDefault() {
        prefs.edit().remove(KEY_URL).apply()
    }

    fun clearBrowsingData(onComplete: (() -> Unit)? = null) {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            onComplete?.invoke()
        }
    }

    private fun isValidHttpsUrl(url: String): Boolean {
        if (!url.startsWith("https://", ignoreCase = true)) return false
        return Patterns.WEB_URL.matcher(url).matches()
    }
}
