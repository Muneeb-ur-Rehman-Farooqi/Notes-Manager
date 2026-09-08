package com.muneeb.coursemanager.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["categoryId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Item(
    @PrimaryKey(autoGenerate = true)
    val itemId: Long = 0,
    val categoryId: Long,
    val itemType: ItemType,
    val displayName: String,
    val uri: String? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val noteText: String? = null,
    val sortOrder: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long? = null
)