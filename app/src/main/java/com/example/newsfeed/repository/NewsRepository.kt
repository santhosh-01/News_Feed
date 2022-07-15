package com.example.newsfeed.repository

import androidx.lifecycle.LiveData
import com.example.newsfeed.api.RetrofitInstance
import com.example.newsfeed.db.ArticleDatabase
import com.example.newsfeed.entity.Article
import com.example.newsfeed.entity.NewsArticle
import retrofit2.Response

class NewsRepository(
    val db: ArticleDatabase
//    val onFetchArticleListener: OnFetchArticleListener<NewsArticle>
) {

    suspend fun getBreakingNews(
        pageNumber: Int,
        category: String,
        countryAbbr: String = ""
    ): Response<NewsArticle> {
        if (countryAbbr == "") {
            return RetrofitInstance.api.getTopHeadlines(
                pageNumber = pageNumber,
                category = category
            )
        } else {
            return RetrofitInstance.api.getTopHeadlinesWithCountry(
                pageNumber = pageNumber,
                category = category,
                country = countryAbbr
            )
        }
    }

    suspend fun searchNews(
        pageNumber: Int,
        searchQuery: String,
        sortBy: String
    ): Response<NewsArticle> {
        if (sortBy == "") {
            return RetrofitInstance.api.searchForNews(pageNumber = pageNumber, query = searchQuery)
        } else {
            return RetrofitInstance.api.searchForNews(
                pageNumber = pageNumber,
                query = searchQuery,
                sortBy = sortBy
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