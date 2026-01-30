package com.example.webviewapp.ui

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.webviewapp.R
import com.example.webviewapp.data.OpenMode
import com.example.webviewapp.util.AppPrefs
import com.example.webviewapp.util.BrowserChooser
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class AppSettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var spinnerDefaultMode: Spinner
    private lateinit var spinnerPreferredBrowser: Spinner
    private lateinit var switchConfirmHttp: SwitchMaterial
    private lateinit var btnClearData: MaterialButton
    private lateinit var appPrefs: AppPrefs

    private var browserPackages: List<String?> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)

        appPrefs = AppPrefs(this)
        initViews()
        loadSettings()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        spinnerDefaultMode = findViewById(R.id.spinner_default_mode)
        spinnerPreferredBrowser = findViewById(R.id.spinner_preferred_browser)
        switchConfirmHttp = findViewById(R.id.switch_confirm_http)
        btnClearData = findViewById(R.id.btn_clear_data)

        toolbar.setNavigationOnClickListener { finish() }

        val items = listOf(
            getString(R.string.open_mode_custom_tabs),
            getString(R.string.open_mode_external),
            getString(R.string.open_mode_webview)
        )
        spinnerDefaultMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)

        setupPreferredBrowserSpinner()
        btnClearData.setOnClickListener { confirmClearData() }
    }

    private fun setupPreferredBrowserSpinner() {
        val entries = BrowserChooser.list(this)
        val labels = buildList {
            add(getString(R.string.preferred_browser_system_default))
            addAll(entries.map { it.label })
        }
        browserPackages = buildList {
            add(null)
            addAll(entries.map { it.packageName })
        }

        spinnerPreferredBrowser.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
    }

    private fun loadSettings() {
        spinnerDefaultMode.setSelection(openModeToIndex(appPrefs.defaultOpenMode))
        val preferredPkg = appPrefs.preferredBrowserPackage
        val browserIndex = browserPackages.indexOfFirst { it == preferredPkg }.let { if (it >= 0) it else 0 }
        spinnerPreferredBrowser.setSelection(browserIndex)
        switchConfirmHttp.isChecked = appPrefs.confirmHttpEveryTime
    }

    override fun onPause() {
        super.onPause()
        appPrefs.defaultOpenMode = indexToOpenMode(spinnerDefaultMode.selectedItemPosition)
        val selected = spinnerPreferredBrowser.selectedItemPosition
        appPrefs.preferredBrowserPackage = browserPackages.getOrNull(selected)
        appPrefs.confirmHttpEveryTime = switchConfirmHttp.isChecked
    }

    private fun confirmClearData() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_clear_data)
            .setPositiveButton(R.string.dialog_confirm) { _, _ -> clearData() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun clearData() {
        val tempWebView = WebView(this)
        tempWebView.clearCache(true)
        tempWebView.destroy()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            runOnUiThread {
                Toast.makeText(this, R.string.data_cleared, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openModeToIndex(mode: OpenMode): Int = when (mode) {
        OpenMode.CUSTOM_TABS -> 0
        OpenMode.EXTERNAL_BROWSER -> 1
        OpenMode.IN_APP_WEBVIEW -> 2
    }

    private fun indexToOpenMode(index: Int): OpenMode = when (index) {
        1 -> OpenMode.EXTERNAL_BROWSER
        2 -> OpenMode.IN_APP_WEBVIEW
        else -> OpenMode.CUSTOM_TABS
    }
}
