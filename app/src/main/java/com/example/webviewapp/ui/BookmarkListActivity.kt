package com.example.webviewapp.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.webviewapp.R
import com.example.webviewapp.config.UrlConfigManager
import com.example.webviewapp.data.AppDatabase
import com.example.webviewapp.data.Bookmark
import com.example.webviewapp.data.BookmarkRepository
import com.example.webviewapp.data.OpenMode
import com.example.webviewapp.util.AppPrefs
import com.example.webviewapp.util.CustomTabsOpener
import com.example.webviewapp.util.UrlValidator
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookmarkListActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutEmpty: View
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var adapter: BookmarkAdapter
    private lateinit var repository: BookmarkRepository
    private lateinit var appPrefs: AppPrefs

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var collectJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookmark_list)

        appPrefs = AppPrefs(this)
        repository = BookmarkRepository(AppDatabase.getInstance(this).bookmarkDao())

        initViews()
        migrateLegacyUrl()
        observeBookmarks(null)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recycler_view)
        layoutEmpty = findViewById(R.id.layout_empty)
        fabAdd = findViewById(R.id.fab_add)

        setSupportActionBar(toolbar)

        adapter = BookmarkAdapter(
            onItemClick = { bookmark -> openBookmark(bookmark) },
            onItemLongClick = { bookmark, view -> showContextMenu(bookmark, view) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fabAdd.setOnClickListener {
            startActivity(Intent(this, BookmarkEditActivity::class.java))
        }
    }

    private fun migrateLegacyUrl() {
        if (appPrefs.didMigrateLegacyUrl) return
        val legacyPrefs = getSharedPreferences("url_config", MODE_PRIVATE)
        val legacyUrl = legacyPrefs.getString("target_url", null)
        if (legacyUrl != null && legacyUrl != UrlConfigManager.DEFAULT_URL) {
            val normalized = UrlValidator.normalize(legacyUrl)
            if (UrlValidator.isValidScheme(normalized)) {
                lifecycleScope.launch {
                    repository.addBookmark("", normalized, null, null)
                }
            }
        }
        appPrefs.didMigrateLegacyUrl = true
    }

    private fun observeBookmarks(query: String?) {
        collectJob?.cancel()
        collectJob = lifecycleScope.launch {
            val flow = if (query.isNullOrBlank()) {
                repository.observeBookmarks()
            } else {
                repository.observeSearch(query)
            }
            flow.collectLatest { list ->
                adapter.submitList(list)
                layoutEmpty.isVisible = list.isEmpty()
            }
        }
    }

    private fun openBookmark(bookmark: Bookmark) {
        val mode = bookmark.openMode ?: appPrefs.defaultOpenMode
        val url = bookmark.url
        val browserPackage = bookmark.browserPackage ?: appPrefs.preferredBrowserPackage

        if (UrlValidator.isHttp(url) && appPrefs.confirmHttpEveryTime) {
            showHttpWarning { doOpen(url, mode, browserPackage) }
        } else {
            doOpen(url, mode, browserPackage)
        }
    }

    private fun doOpen(url: String, mode: OpenMode, browserPackage: String?) {
        when (mode) {
            OpenMode.CUSTOM_TABS -> CustomTabsOpener.open(this, url, browserPackage)
            OpenMode.EXTERNAL_BROWSER -> {
                val uri = android.net.Uri.parse(url)
                if (!browserPackage.isNullOrBlank()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(browserPackage))
                        return
                    } catch (_: Exception) {
                        // fall back to system default
                    }
                }
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {
                    Toast.makeText(this, R.string.error_open_failed, Toast.LENGTH_SHORT).show()
                }
            }
            OpenMode.IN_APP_WEBVIEW -> {
                startActivity(Intent(this, WebViewActivity::class.java).apply {
                    putExtra(WebViewActivity.EXTRA_URL, url)
                    putExtra(WebViewActivity.EXTRA_BROWSER_PACKAGE, browserPackage)
                })
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

    private fun showContextMenu(bookmark: Bookmark, anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(R.string.menu_edit)
            menu.add(R.string.menu_delete)
            setOnMenuItemClickListener { item ->
                when (item.title) {
                    getString(R.string.menu_edit) -> {
                        startActivity(Intent(this@BookmarkListActivity, BookmarkEditActivity::class.java).apply {
                            putExtra(BookmarkEditActivity.EXTRA_BOOKMARK_ID, bookmark.id)
                        })
                        true
                    }
                    getString(R.string.menu_delete) -> {
                        confirmDelete(bookmark)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun confirmDelete(bookmark: Bookmark) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirm_delete_bookmark, bookmark.title))
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                lifecycleScope.launch { repository.deleteBookmark(bookmark.id) }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bookmark_list_menu, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable { observeBookmarks(newText) }
                handler.postDelayed(searchRunnable!!, 300)
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, AppSettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroy()
    }
}