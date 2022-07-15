package com.example.newsfeed.util.listener

import com.example.newsfeed.entity.Article
import com.google.android.material.card.MaterialCardView

interface OnArticleClickListener {

    fun onClick(article: Article)

    fun onBookmarkButtonClick(article: Article)

    fun onShareButtonClick(article: Article)

    fun onLongClick(article: Article, cardView: MaterialCardView)

}