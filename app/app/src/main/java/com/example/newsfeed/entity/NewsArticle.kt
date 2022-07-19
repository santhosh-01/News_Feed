package com.example.newsfeed.entity

data class NewsArticle(
    val articles: MutableList<Article>,
    val status: String,
    val totalResults: Int
)