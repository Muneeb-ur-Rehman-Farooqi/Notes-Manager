package com.muneeb.coursemanager.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
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
data class Page(
    @PrimaryKey(autoGenerate = true)
    val pageId: Long = 0,
    val itemId: Long,
    val photoUri: String,
    val pageNumber: Int
)