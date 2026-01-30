package com.example.webviewapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<Bookmark>>

    @Query("""
        SELECT * FROM bookmarks
        WHERE title LIKE '%' || :q || '%' COLLATE NOCASE
           OR url LIKE '%' || :q || '%' COLLATE NOCASE
        ORDER BY orderIndex
    """)
    fun observeSearch(q: String): Flow<List<Bookmark>>

    @Query("SELECT MAX(orderIndex) FROM bookmarks")
    suspend fun maxOrderIndex(): Int?

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getById(id: String): Bookmark?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(bookmark: Bookmark)

    @Update
    suspend fun update(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteById(id: String)
}
