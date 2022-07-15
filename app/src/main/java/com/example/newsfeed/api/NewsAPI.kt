package com.example.newsfeed.api

import com.example.newsfeed.entity.NewsArticle
import com.example.newsfeed.util.Constants
import org.intellij.lang.annotations.Language
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsAPI {

    @GET("/v2/everything")
    suspend fun searchForNews(
        @Query("apiKey")
        apiKey: String = Constants.API_KEY,
        @Query("q")
        query: String,
        @Query("sortBy")
        sortBy: String = "relevancy",
        @Query("language")
        language: String = "en",
        @Query("page")
        pageNumber: Int = 1,
        @Query("pageSize")
        pageSize: Int = Constants.QUERY_PAGE_SIZE
    ): Response<NewsArticle>

    @GET("/v2/top-headlines")
    suspend fun getTopHeadlines(
        @Query("apiKey")
        apiKey: String = Constants.API_KEY,
        @Query("category")
        category: String = "general",
        @Query("page")
        pageNumber: Int = 1,
        @Query("pageSize")
        pageSize: Int = Constants.QUERY_PAGE_SIZE
    ): Response<NewsArticle>

    @GET("/v2/top-headlines")
    suspend fun getTopHeadlinesWithCountry(
        @Query("apiKey")
        apiKey: String = Constants.API_KEY,
        @Query("category")
        category: String = "general",
        @Query("country")
        country: String,
        @Query("page")
        pageNumber: Int = 1,
        @Query("pageSize")
        pageSize: Int = Constants.QUERY_PAGE_SIZE
    ): Response<NewsArticle>

}