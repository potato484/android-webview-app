package com.example.webviewapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import com.example.webviewapp.config.UrlConfigManager
import com.example.webviewapp.util.NetworkChecker
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {

    private lateinit var webViewContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorView: View
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button
    private lateinit var urlConfigManager: UrlConfigManager

    private var isLoading = false
    private var currentHost: String? = null

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            loadUrl()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlConfigManager = UrlConfigManager(this)
        initViews()
        setupWebView()

        if (NetworkChecker.isNetworkAvailable(this)) {
            loadUrl()
        } else {
            showError(getString(R.string.error_no_internet))
        }
    }

    private fun initViews() {
        webViewContainer = findViewById(R.id.webview_container)
        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progress)
        errorView = findViewById(R.id.error_view)
        tvErrorMessage = errorView.findViewById(R.id.tv_error_message)
        btnRetry = errorView.findViewById(R.id.btn_retry)

        btnRetry.setOnClickListener {
            if (!isLoading) retry()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // Some sites block Android WebView based on the default UA tokens (e.g. "; wv", "Version/4.0").
        // Keep it simple: reuse the system UA but strip the WebView markers.
        webView.settings.userAgentString = webView.settings.userAgentString
            .replace("; wv", "")
            .replace("Version/4.0 ", "")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                val scheme = url.scheme?.lowercase() ?: return true

                return when (scheme) {
                    "javascript", "file", "content" -> true
                    "https" -> {
                        if (url.host == currentHost) return false
                        launchExternalUrl(url)
                        true
                    }
                    else -> {
                        launchExternalUrl(url)
                        true
                    }
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                isLoading = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                isLoading = false
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    showError(getString(R.string.error_load_failed))
                }
            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                if (request?.isForMainFrame == true) {
                    val code = response?.statusCode ?: 0
                    // Many sites (e.g. Cloudflare) return a temporary 403/429 challenge page that
                    // must be displayed so the user can complete verification. Don't block it.
                    if (code in 500..599) {
                        showError(getString(R.string.error_http_failed, code))
                    }
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.ssl_error_title)
                    .setMessage(R.string.ssl_error_message)
                    .setPositiveButton(R.string.ssl_proceed) { _, _ -> handler?.proceed() }
                    .setNegativeButton(R.string.ssl_cancel) { _, _ -> handler?.cancel() }
                    .setCancelable(false)
                    .show()
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                recreateWebView()
                showError(getString(R.string.error_load_failed))
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progressBar.isVisible = true
                    progressBar.progress = newProgress
                } else {
                    progressBar.isVisible = false
                }
            }
        }
    }

    private fun loadUrl() {
        if (!NetworkChecker.isNetworkAvailable(this)) {
            showError(getString(R.string.error_no_internet))
            return
        }
        hideError()
        webView.stopLoading()
        isLoading = true
        val url = urlConfigManager.getUrl()
        currentHost = Uri.parse(url).host
        webView.loadUrl(url)
    }

    private fun retry() {
        if (NetworkChecker.isNetworkAvailable(this)) {
            loadUrl()
        }
    }

    private fun showError(message: String) {
        isLoading = false
        progressBar.isVisible = false
        tvErrorMessage.text = message
        errorView.isVisible = true
        webView.isVisible = false
    }

    private fun hideError() {
        errorView.isVisible = false
        webView.isVisible = true
    }

    private fun launchExternalUrl(url: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: Exception) {
            // No app can handle this URL or security exception
        }
    }

    private fun recreateWebView() {
        webViewContainer.removeView(webView)
        webView.destroy()
        webView = WebView(this).apply {
            id = R.id.webview
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        webViewContainer.addView(webView, 0)
        setupWebView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                settingsLauncher.launch(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
        super.onDestroy()
    }
}
