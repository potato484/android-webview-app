package com.example.webviewapp.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.example.webviewapp.R

object CustomTabsOpener {
    fun open(context: Context, url: String, browserPackage: String? = null) {
        try {
            val customTabs = CustomTabsIntent.Builder().build()
            if (!browserPackage.isNullOrBlank()) {
                customTabs.intent.`package` = browserPackage
            }
            customTabs.launchUrl(context, Uri.parse(url))
        } catch (_: ActivityNotFoundException) {
            fallbackToActionView(context, url, browserPackage)
        }
    }

    private fun fallbackToActionView(context: Context, url: String, browserPackage: String?) {
        try {
            val uri = Uri.parse(url)
            if (!browserPackage.isNullOrBlank()) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(browserPackage))
                    return
                } catch (_: Exception) {
                    // fall back to system default
                }
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: Exception) {
            Toast.makeText(context, R.string.error_open_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
