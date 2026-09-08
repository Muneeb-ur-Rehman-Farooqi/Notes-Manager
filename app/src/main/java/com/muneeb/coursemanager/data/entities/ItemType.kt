package com.muneeb.coursemanager.data.entities

import androidx.room.TypeConverter

enum class ItemType {
    FILE,
    NOTE,
    PHOTO_GROUP
}

class Converters {
    @TypeConverter
    fun fromItemType(type: ItemType): String = type.name

    @TypeConverter
    fun toItemType(name: String): ItemType = ItemType.valueOf(name)
}