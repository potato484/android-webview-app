package com.example.webviewapp.util

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.webviewapp.R

object BrowserChooser {

    data class Entry(
        val label: String,
        val packageName: String,
    )

    fun choose(
        activity: AppCompatActivity,
        url: String,
        title: String? = null,
        onSelected: (Entry) -> Unit,
    ) {
        val uri = Uri.parse(url)
        val entries = list(activity, uri)
        if (entries.isEmpty()) {
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                Toast.makeText(activity, R.string.error_open_failed, Toast.LENGTH_SHORT).show()
            }
            return
        }

        val labels = entries.map { it.label }.toTypedArray()
        AlertDialog.Builder(activity)
            .setTitle(title ?: activity.getString(R.string.browser_chooser_title))
            .setItems(labels) { _, which ->
                entries.getOrNull(which)?.let(onSelected)
            }
            .setNegativeButton(R.string.browser_chooser_cancel, null)
            .show()
    }

    private val KNOWN_BROWSERS = listOf(
        "com.microsoft.emmx" to "Microsoft Edge",
        "com.tencent.mtt" to "QQ浏览器",
        "com.UCMobile" to "UC浏览器",
        "com.android.chrome" to "Chrome",
        "org.mozilla.firefox" to "Firefox",
    )

    fun list(context: Context, uri: Uri = Uri.parse("https://example.com")): List<Entry> {
        val scheme = uri.scheme?.lowercase()
        val uris = if (scheme == "http" || scheme == "https") {
            listOf(
                Uri.parse("https://example.com"),
                Uri.parse("http://example.com"),
            )
        } else {
            listOf(uri)
        }

        val pm = context.packageManager
        val fromQuery = uris
            .flatMap { queryForUri(pm, it) }
            .distinctBy { it.packageName }

        // 白名单兜底：检查已知浏览器是否安装
        val fromWhitelist = KNOWN_BROWSERS.mapNotNull { (pkg, defaultLabel) ->
            try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                val label = pm.getApplicationLabel(appInfo)?.toString() ?: defaultLabel
                Entry(label = label, packageName = pkg)
            } catch (_: PackageManager.NameNotFoundException) {
                null
            }
        }

        return (fromQuery + fromWhitelist)
            .distinctBy { it.packageName }
            .sortedBy { it.label }
    }

    private fun queryForUri(pm: PackageManager, uri: Uri): List<Entry> {
        val intents = listOf(
            Intent(Intent.ACTION_VIEW, uri),
            Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE),
            Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_DEFAULT),
        )

        return intents
            .flatMap { intent ->
                val resolveInfos = if (Build.VERSION.SDK_INT >= 33) {
                    pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryIntentActivities(intent, 0)
                }
                resolveInfos.mapNotNull { ri ->
                    val packageName = ri.activityInfo?.packageName ?: return@mapNotNull null
                    val label = ri.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: packageName
                    Entry(label = label, packageName = packageName)
                }
            }
    }
}
