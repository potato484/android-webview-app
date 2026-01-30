package com.example.webviewapp.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.webviewapp.R
import com.example.webviewapp.util.AppPrefs
import com.example.webviewapp.util.NetworkChecker
import com.google.android.material.progressindicator.LinearProgressIndicator

class WebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_BROWSER_PACKAGE = "extra_browser_package"
    }

    private lateinit var webViewContainer: FrameLayout
    private lateinit var webView: WebView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorView: View
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: Button

    private lateinit var appPrefs: AppPrefs

    private var isLoading = false
    private var currentHost: String? = null
    private var targetUrl: String? = null
    private var browserPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        appPrefs = AppPrefs(this)
        targetUrl = intent.getStringExtra(EXTRA_URL)
        browserPackage = intent.getStringExtra(EXTRA_BROWSER_PACKAGE)?.takeIf { it.isNotBlank() }
        if (targetUrl == null) {
            finish()
            return
        }

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
                    if (code in 500..599) {
                        showError(getString(R.string.error_http_failed, code))
                    }
                }
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                AlertDialog.Builder(this@WebViewActivity)
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
        currentHost = Uri.parse(targetUrl).host
        webView.loadUrl(targetUrl!!)
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
            val scheme = url.scheme?.lowercase()
            val pkg = browserPackage ?: appPrefs.preferredBrowserPackage
            if ((scheme == "http" || scheme == "https") && !pkg.isNullOrBlank()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, url).setPackage(pkg))
                    return
                } catch (_: Exception) {
                    // fall back to system default
                }
            }
            startActivity(Intent(Intent.ACTION_VIEW, url))
        } catch (_: Exception) {}
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
