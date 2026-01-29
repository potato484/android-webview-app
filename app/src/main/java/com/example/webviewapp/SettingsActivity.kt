package com.example.webviewapp

import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.webviewapp.config.UrlConfigManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tilUrl: TextInputLayout
    private lateinit var etUrl: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var btnClearData: MaterialButton
    private lateinit var urlConfigManager: UrlConfigManager

    private var initialUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        urlConfigManager = UrlConfigManager(this)
        initViews()
        loadCurrentUrl()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tilUrl = findViewById(R.id.til_url)
        etUrl = findViewById(R.id.et_url)
        btnSave = findViewById(R.id.btn_save)
        btnReset = findViewById(R.id.btn_reset)
        btnClearData = findViewById(R.id.btn_clear_data)

        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        btnSave.setOnClickListener { saveUrl() }
        btnReset.setOnClickListener { resetToDefault() }
        btnClearData.setOnClickListener { confirmClearData() }
    }

    private fun loadCurrentUrl() {
        initialUrl = urlConfigManager.getUrl()
        etUrl.setText(initialUrl)
    }

    private fun saveUrl() {
        val url = etUrl.text?.toString()?.trim() ?: ""

        when {
            url.isEmpty() -> {
                tilUrl.error = getString(R.string.error_invalid_url)
            }
            !url.startsWith("https://", ignoreCase = true) -> {
                tilUrl.error = getString(R.string.error_https_required)
            }
            !urlConfigManager.saveUrl(url) -> {
                tilUrl.error = getString(R.string.error_invalid_url)
            }
            else -> {
                tilUrl.error = null
                Toast.makeText(this, R.string.url_saved, Toast.LENGTH_SHORT).show()
                if (url != initialUrl) {
                    setResult(RESULT_OK)
                }
                finish()
            }
        }
    }

    private fun resetToDefault() {
        urlConfigManager.resetToDefault()
        etUrl.setText(UrlConfigManager.DEFAULT_URL)
        tilUrl.error = null
        Toast.makeText(this, R.string.url_reset, Toast.LENGTH_SHORT).show()
        if (initialUrl != UrlConfigManager.DEFAULT_URL) {
            setResult(RESULT_OK)
        }
    }

    private fun confirmClearData() {
        AlertDialog.Builder(this)
            .setMessage(R.string.confirm_clear_data)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                val tempWebView = WebView(this)
                tempWebView.clearCache(true)
                tempWebView.destroy()
                urlConfigManager.clearBrowsingData {
                    runOnUiThread {
                        Toast.makeText(this, R.string.data_cleared, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
