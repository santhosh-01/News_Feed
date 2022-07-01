package com.example.newsfeed.home

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.R
import com.example.newsfeed.entity.Article
import com.example.newsfeed.entity.NewsArticle
import com.example.newsfeed.repository.NewsRepository
import com.example.newsfeed.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response

class NewsViewModel(
    val application: Application,
    val newsRepository: NewsRepository
) : ViewModel() {

    var selectedCategory: String = "general"

    var breakingNewsResponse: NewsArticle? = null
    var searchNewsResponse: NewsArticle? = null

    var breakingNewsPage: Int = 1
    var searchNewsPage: Int = 1

    val articleList: MutableList<Article> = mutableListOf()

    val breakingNews: MutableLiveData<Resource<NewsArticle>> = MutableLiveData()

    val searchNews: MutableLiveData<Resource<NewsArticle>> = MutableLiveData()

    val selectedNewsListInBookmarks: MutableList<Article> = mutableListOf()
    val selectedNewsListInHome: MutableList<Article> = mutableListOf()

    val selectedItemPositionsInHome: MutableLiveData<List<Int>> = MutableLiveData(listOf())
    val selectedItemPositionsInBookmark: MutableLiveData<List<Int>> = MutableLiveData(listOf())

    init {
        getBreakingNews(selectedCategory)
    }

    fun initBreakingNews() {
        breakingNewsPage = 1
        getBreakingNews(selectedCategory)
    }

    fun getBreakingNews(category: String) = viewModelScope.launch {
        breakingNews.postValue(Resource.Loading())
        val response: Response<NewsArticle> = newsRepository.getBreakingNews(breakingNewsPage, category)
        breakingNews.postValue(handleBreakingNewsResponse(response))
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        searchNews.postValue(Resource.Loading())
        val response: Response<NewsArticle> = newsRepository.searchNews(searchQuery)
        searchNews.postValue(handleSearchNewsResponse(response))
    }

    private fun handleBreakingNewsResponse(response: Response<NewsArticle>) : Resource<NewsArticle> {
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
                breakingNewsPage++
                if (breakingNewsResponse == null) {
                    breakingNewsResponse = resultResponse
                }
                else {
                    val oldArticles = breakingNewsResponse?.articles
                    val newArticles = resultResponse.articles

                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(breakingNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsArticle>) : Resource<NewsArticle> {
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
                searchNewsPage ++
                if (searchNewsResponse == null) {
                    searchNewsResponse = resultResponse
                }
                else {
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = resultResponse.articles

                    oldArticles?.addAll(newArticles)
                }
                return Resource.Success(searchNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    suspend fun insertArticle(article: Article): Long {
        return newsRepository.insertArticle(article)
    }

    fun getSavedNewsArticles() = newsRepository.getSavedNewsArticles()

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsRepository.deleteArticle(article)
    }

    fun isRecordExist(title: String) = newsRepository.isRecordExist(title)

    fun getArticleByTitle(title: String) = newsRepository.getArticleByTitle(title)

    fun addSelectedItemPositionInHome(position: Int) {
        selectedItemPositionsInHome.value = selectedItemPositionsInHome.value!!.plus(position)
    }

    fun removeUnselectedItemPositionFromHome(position: Int) {
        selectedItemPositionsInHome.value = selectedItemPositionsInHome.value!!.minus(position)
    }

    fun addSelectedItemPositionInBookmark(position: Int) {
        selectedItemPositionsInBookmark.value = selectedItemPositionsInBookmark.value!!.plus(position)
    }

    fun removeUnselectedItemPositionFromBookmark(position: Int) {
        selectedItemPositionsInBookmark.value = selectedItemPositionsInBookmark.value!!.minus(position)
    }

    fun clearSelectedItemPositionListInHome() {
        selectedItemPositionsInHome.value = listOf()
    }

    fun clearSelectedItemPositionListInBookmark() {
        selectedItemPositionsInBookmark.value = listOf()
    }

    fun unSelectAllArticles() {
        selectedNewsListInHome.forEach { article ->
            article.isChecked = false
        }
        selectedNewsListInHome.clear()
        clearSelectedItemPositionListInHome()
    }
}