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

    suspend fun getBreakingNews(pageNumber: Int, category: String): Response<NewsArticle> {
        val response: Response<NewsArticle> = RetrofitInstance.api.getTopHeadlines(pageNumber = pageNumber, category = category)
//        if (response.isSuccessful) onFetchArticleListener.onFetchData(response.body()!!.articles)
//        else onFetchArticleListener.onError("Request is unsuccessful!!")
        return response
    }

    suspend fun searchNews(searchQuery: String): Response<NewsArticle> {
        return RetrofitInstance.api.searchForNews(query = searchQuery)
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