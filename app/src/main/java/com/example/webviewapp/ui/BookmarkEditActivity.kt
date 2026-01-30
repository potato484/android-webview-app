package com.example.webviewapp.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.view.isVisible
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.webviewapp.R
import com.example.webviewapp.data.AppDatabase
import com.example.webviewapp.data.BookmarkRepository
import com.example.webviewapp.data.OpenMode
import com.example.webviewapp.util.BrowserChooser
import com.example.webviewapp.util.UrlValidator
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class BookmarkEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOKMARK_ID = "bookmark_id"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilUrl: TextInputLayout
    private lateinit var etUrl: TextInputEditText
    private lateinit var spinnerOpenMode: Spinner
    private lateinit var spinnerBrowser: Spinner
    private lateinit var btnSave: MaterialButton
    private lateinit var repository: BookmarkRepository

    private var bookmarkId: String? = null
    private var originalUrl: String? = null
    private var browserPackages: List<String?> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_edit)

        repository = BookmarkRepository(AppDatabase.getInstance(this).bookmarkDao())
        bookmarkId = intent.getStringExtra(EXTRA_BOOKMARK_ID)

        initViews()
        setupSpinner()
        setupBrowserSpinner()
        if (bookmarkId != null) loadBookmark()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tilTitle = findViewById(R.id.til_title)
        etTitle = findViewById(R.id.et_title)
        tilUrl = findViewById(R.id.til_url)
        etUrl = findViewById(R.id.et_url)
        spinnerOpenMode = findViewById(R.id.spinner_open_mode)
        spinnerBrowser = findViewById(R.id.spinner_browser)
        btnSave = findViewById(R.id.btn_save)

        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = if (bookmarkId == null) getString(R.string.add_bookmark) else getString(R.string.edit_bookmark)
        btnSave.setOnClickListener { save() }
    }

    private fun setupSpinner() {
        val items = listOf(
            getString(R.string.open_mode_default),
            getString(R.string.open_mode_custom_tabs),
            getString(R.string.open_mode_external),
            getString(R.string.open_mode_webview)
        )
        spinnerOpenMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        spinnerOpenMode.setSelection(0)
        spinnerOpenMode.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val mode = indexToOpenMode(position)
                val showBrowser = mode != OpenMode.IN_APP_WEBVIEW
                spinnerBrowser.isEnabled = showBrowser
                spinnerBrowser.isVisible = true
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun setupBrowserSpinner() {
        val entries = BrowserChooser.list(this)
        val labels = buildList {
            add(getString(R.string.bookmark_browser_follow_global))
            addAll(entries.map { it.label })
        }
        browserPackages = buildList {
            add(null)
            addAll(entries.map { it.packageName })
        }

        spinnerBrowser.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        spinnerBrowser.setSelection(0)
    }

    private fun loadBookmark() {
        lifecycleScope.launch {
            val bookmark = repository.getById(bookmarkId!!)
            if (bookmark == null) {
                Toast.makeText(this@BookmarkEditActivity, R.string.error_bookmark_not_found, Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            etTitle.setText(bookmark.title)
            etUrl.setText(bookmark.url)
            originalUrl = bookmark.url
            spinnerOpenMode.setSelection(openModeToIndex(bookmark.openMode))
            val idx = browserPackages.indexOfFirst { it == bookmark.browserPackage }.coerceAtLeast(0)
            spinnerBrowser.setSelection(idx)
        }
    }

    private fun save() {
        val title = etTitle.text?.toString()?.trim() ?: ""
        val rawUrl = etUrl.text?.toString()?.trim() ?: ""

        if (rawUrl.isEmpty()) {
            tilUrl.error = getString(R.string.error_invalid_url)
            return
        }

        val normalizedUrl = UrlValidator.normalize(rawUrl)
        if (!UrlValidator.isValidScheme(normalizedUrl)) {
            Toast.makeText(this, R.string.error_invalid_scheme, Toast.LENGTH_SHORT).show()
            return
        }

        val openMode = indexToOpenMode(spinnerOpenMode.selectedItemPosition)
        val selectedBrowserPackage = browserPackages.getOrNull(spinnerBrowser.selectedItemPosition)
        val isNewHttp = UrlValidator.isHttp(normalizedUrl) && (originalUrl == null || !UrlValidator.isHttp(originalUrl!!))

        if (isNewHttp) {
            showHttpWarning { doSave(title, rawUrl, openMode, selectedBrowserPackage) }
        } else {
            doSave(title, rawUrl, openMode, selectedBrowserPackage)
        }
    }

    private fun doSave(title: String, rawUrl: String, openMode: OpenMode?, browserPackage: String?) {
        lifecycleScope.launch {
            val result = if (bookmarkId == null) {
                repository.addBookmark(title, rawUrl, openMode, browserPackage)
            } else {
                repository.updateBookmark(bookmarkId!!, title, rawUrl, openMode, browserPackage)
            }
            when (result) {
                is BookmarkRepository.Result.Success -> finish()
                is BookmarkRepository.Result.DuplicateUrl -> {
                    Toast.makeText(this@BookmarkEditActivity, R.string.error_duplicate_url, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showHttpWarning(onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(R.string.http_warning_title)
            .setMessage(R.string.http_warning_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun openModeToIndex(mode: OpenMode?): Int = when (mode) {
        null -> 0
        OpenMode.CUSTOM_TABS -> 1
        OpenMode.EXTERNAL_BROWSER -> 2
        OpenMode.IN_APP_WEBVIEW -> 3
    }

    private fun indexToOpenMode(index: Int): OpenMode? = when (index) {
        1 -> OpenMode.CUSTOM_TABS
        2 -> OpenMode.EXTERNAL_BROWSER
        3 -> OpenMode.IN_APP_WEBVIEW
        else -> null
    }
}
