package com.example.webviewapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

@Database(entities = [Bookmark::class], version = 2, exportSchema = false)
@TypeConverters(OpenModeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN browserPackage TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "bookmarks.db"
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val id = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        "INSERT INTO bookmarks (id, title, url, createdAt, updatedAt, orderIndex, openMode) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(id, "Linux.do", "https://linux.do", now, now, 0, null)
                    )
                }
            }).addMigrations(MIGRATION_1_2).build()
        }
    }
}
