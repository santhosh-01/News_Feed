package com.example.newsfeed.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "article",
    indices = [Index(value = ["author", "content", "description", "publishedAt", "source", "title", "url", "urlToImage"], unique = true)]
)
data class Article(
    @PrimaryKey(autoGenerate = true)
    var id: Int? = null,
    val author: String? = null,
    val content: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
    val source: Source? = null,
    val title: String,
    val url: String? = null,
    val urlToImage: String? = null,

    var isChecked: Boolean = false,
    var isExistInDB: Boolean = false
): Serializable