package com.example.newsfeed.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.newsfeed.entity.Article

@Dao
interface ArticleDao {

    @Query("SELECT * FROM article")
    fun getSavedNewsArticles() : LiveData<List<Article>>

    @Query("SELECT EXISTS(SELECT * FROM article WHERE title = :title)")
    fun isRecordExist(title : String) : Boolean

    @Query("SELECT * FROM article WHERE title = :title")
    fun getArticleByTitle(title: String) : Article

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article): Long

    @Delete
    suspend fun deleteArticle(article: Article)

}