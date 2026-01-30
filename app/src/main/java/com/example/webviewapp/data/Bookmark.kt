package com.example.webviewapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bookmarks", indices = [Index("url", unique = true)])
data class Bookmark(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0,
    val openMode: OpenMode? = null,
    /**
     * Preferred browser package name for this bookmark when using system browser modes.
     * `null` means follow global preferred browser (or system default if global is also null).
     */
    val browserPackage: String? = null
)
