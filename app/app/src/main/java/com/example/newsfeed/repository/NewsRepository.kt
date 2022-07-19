package com.example.newsfeed.repository

import androidx.lifecycle.LiveData
import com.example.newsfeed.api.RetrofitInstance
import com.example.newsfeed.db.ArticleDatabase
import com.example.newsfeed.entity.Article
import com.example.newsfeed.entity.NewsArticle
import retrofit2.Response

class NewsRepository(
    val db: ArticleDatabase
) {

    suspend fun getBreakingNews(
        pageNumber: Int,
        category: String,
        countryAbbr: String,
        apiKey: String
    ): Response<NewsArticle> {
        if (countryAbbr == "global") {
            return RetrofitInstance.api.getTopHeadlines(
                pageNumber = pageNumber,
                category = category,
                apiKey = apiKey
            )
        } else {
            return RetrofitInstance.api.getTopHeadlinesWithCountry(
                pageNumber = pageNumber,
                category = category,
                country = countryAbbr,
                apiKey = apiKey
            )
        }
    }

    suspend fun searchNews(
        pageNumber: Int,
        searchQuery: String,
        sortBy: String,
        language: String,
        apiKey: String
    ): Response<NewsArticle> {
        if (sortBy == "") {
            return RetrofitInstance.api.searchForNews(pageNumber = pageNumber, query = searchQuery, language = language, apiKey = apiKey)
        } else {
            return RetrofitInstance.api.searchForNews(
                pageNumber = pageNumber,
                query = searchQuery,
                sortBy = sortBy,
                language = language,
                apiKey = apiKey
            )
        }
    }

    suspend fun insertArticle(article: Article): Long {
        return db.getArticleDao().insertArticle(article)
    }

    fun getSavedNewsArticles(): LiveData<List<Article>> {
        return db.getArticleDao().getSavedNewsArticles()
    }

    suspend fun deleteArticle(article: Article) {
        db.getArticleDao().deleteArticle(article)
    }

    fun isRecordExist(title: String): Boolean {
        return db.getArticleDao().isRecordExist(title)
    }

    fun getArticleByTitle(title: String): Article {
        return db.getArticleDao().getArticleByTitle(title)
    }

}