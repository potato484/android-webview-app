package com.example.webviewapp.data

import androidx.room.TypeConverter

class OpenModeConverter {
    @TypeConverter
    fun toText(mode: OpenMode?): String? = mode?.name

    @TypeConverter
    fun fromText(text: String?): OpenMode? = OpenMode.fromString(text)
}
