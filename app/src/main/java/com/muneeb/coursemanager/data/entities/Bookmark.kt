package com.muneeb.coursemanager.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = Item::class,
            parentColumns = ["itemId"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val bookmarkId: Long = 0,
    val itemId: Long,
    val pageNumber: Int,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)