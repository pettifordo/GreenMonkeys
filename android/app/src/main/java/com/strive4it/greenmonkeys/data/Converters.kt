package com.strive4it.greenmonkeys.data

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Room converters. Lists are joined with the ASCII unit separator, which can't
 * appear in user text typed on a phone keyboard; crimes/offsets never contain it.
 */
class Converters {
    private val separator = '\u001F'

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun intListToString(value: List<Int>): String = value.joinToString(separator.toString())

    @TypeConverter
    fun stringToIntList(value: String): List<Int> =
        if (value.isEmpty()) emptyList() else value.split(separator).mapNotNull { it.toIntOrNull() }

    @TypeConverter
    fun stringListToString(value: List<String>): String = value.joinToString(separator.toString())

    @TypeConverter
    fun stringToStringList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(separator)
}
