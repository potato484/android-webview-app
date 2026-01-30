package com.example.webviewapp.data

import android.database.sqlite.SQLiteConstraintException
import com.example.webviewapp.util.UrlValidator
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val dao: BookmarkDao) {

    fun observeBookmarks(): Flow<List<Bookmark>> = dao.observeAll()

    fun observeSearch(query: String): Flow<List<Bookmark>> = dao.observeSearch(query)

    suspend fun getById(id: String): Bookmark? = dao.getById(id)

    suspend fun addBookmark(
        title: String,
        rawUrl: String,
        openMode: OpenMode?,
        browserPackage: String?,
    ): Result {
        val url = UrlValidator.normalize(rawUrl)
        val finalTitle = title.ifBlank { extractHost(url) }
        return try {
            val order = (dao.maxOrderIndex() ?: -1) + 1
            dao.insert(
                Bookmark(
                    title = finalTitle,
                    url = url,
                    orderIndex = order,
                    openMode = openMode,
                    browserPackage = browserPackage
                )
            )
            Result.Success
        } catch (e: SQLiteConstraintException) {
            Result.DuplicateUrl
        }
    }

    suspend fun updateBookmark(
        id: String,
        title: String,
        rawUrl: String,
        openMode: OpenMode?,
        browserPackage: String?,
    ): Result {
        val existing = dao.getById(id) ?: return Result.Success
        val url = UrlValidator.normalize(rawUrl)
        val finalTitle = title.ifBlank { extractHost(url) }
        return try {
            dao.update(existing.copy(
                title = finalTitle,
                url = url,
                updatedAt = System.currentTimeMillis(),
                openMode = openMode,
                browserPackage = browserPackage
            ))
            Result.Success
        } catch (e: SQLiteConstraintException) {
            Result.DuplicateUrl
        }
    }

    suspend fun deleteBookmark(id: String) = dao.deleteById(id)

    private fun extractHost(url: String): String {
        return try {
            java.net.URI(url).host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url
        }
    }

    sealed class Result {
        data object Success : Result()
        data object DuplicateUrl : Result()
    }
}
