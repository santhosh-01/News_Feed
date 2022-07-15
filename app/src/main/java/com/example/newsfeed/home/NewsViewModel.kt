package com.example.newsfeed.home

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.entity.Article
import com.example.newsfeed.entity.NewsArticle
import com.example.newsfeed.repository.NewsRepository
import com.example.newsfeed.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException
import java.util.*

class NewsViewModel(
    val app: Application,
    val newsRepository: NewsRepository
) : AndroidViewModel(app) {

    private val sharedPref: SharedPreferences = app.getSharedPreferences("application", Context.MODE_PRIVATE)

//    var isSearchOn: Boolean = false
//    var breakingNewsAdapter: NewsAdapter? = null

    var selectedCategory: String = ""
    var selectedCountry: String = ""

    var breakingNewsResponse: NewsArticle? = null
    var searchNewsResponse: NewsArticle? = null

    var breakingNewsPage: Int = 0
    var searchNewsPage: Int = 0

    var newSearchQuery:String? = null
    var oldSearchQuery:String? = null

    val searchQueryStack: MutableLiveData<Stack<String>> = MutableLiveData(Stack())

    val articleList: MutableList<Article> = mutableListOf()

    val breakingNews: MutableLiveData<Resource<NewsArticle>> = MutableLiveData()

    val searchNews: MutableLiveData<Resource<NewsArticle>> = MutableLiveData()

    val selectedNewsListInBookmarks: MutableList<Article> = mutableListOf()
    val selectedNewsListInHome: MutableList<Article> = mutableListOf()

    val selectedItemPositionsInHome: MutableLiveData<List<Int>> = MutableLiveData(listOf())
    private val selectedItemPositionsInBookmark: MutableLiveData<List<Int>> = MutableLiveData(listOf())

    fun initBreakingNews() {
        if (sharedPref.getString("country","") == "global") {
            getBreakingNews(sharedPref.getString("category","")!!)
        }
        else {
            getBreakingNews(sharedPref.getString("category","")!!, sharedPref.getString("country","")!!)
        }
    }

    fun initAccordingToCurrentConfig(){
        val currentCategory = sharedPref.getString("category","")
        val currentCountry = sharedPref.getString("country","")
        breakingNewsPage = 0
        breakingNewsResponse = null
        initBreakingNews()
        selectedCategory = currentCategory!!
        selectedCountry = currentCountry!!
    }

    fun getBreakingNews(category: String, countryAbbr: String = "") = viewModelScope.launch {
//        breakingNews.postValue(Resource.Loading())
//        val response: Response<NewsArticle> = newsRepository.getBreakingNews(breakingNewsPage, category, countryAbbr)
//        breakingNews.postValue(handleBreakingNewsResponse(response))

        safeBreakingNewsCall(category, countryAbbr)
    }

    fun searchNews(searchQuery: String, sortBy: String = "") = viewModelScope.launch {
//        searchNews.postValue(Resource.Loading())
//        val response: Response<NewsArticle> = newsRepository.searchNews(searchQuery)
//        searchNews.postValue(handleSearchNewsResponse(response))

        safeSearchNewsCall(searchQuery, sortBy)
    }

    private fun handleBreakingNewsResponse(response: Response<NewsArticle>) : Resource<NewsArticle> {
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
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

    fun clearSelectedItemsInHome() {
        selectedItemPositionsInHome.value = listOf()
        selectedNewsListInHome.clear()
    }

    fun clearSelectedItemsInBookmark() {
        selectedItemPositionsInBookmark.value = listOf()
        selectedNewsListInBookmarks.clear()
    }

    fun unSelectAllArticles() {
        selectedNewsListInHome.forEach { article ->
            article.isChecked = false
        }
        clearSelectedItemsInHome()
    }

    fun pushToSearchQueryStack(value: String) {
        val temp = searchQueryStack.value
        temp!!.push(value)
        searchQueryStack.value = temp
    }

    fun popFromSearchQueryStack(): String {
        val temp = searchQueryStack.value
        val result = temp!!.pop()
        searchQueryStack.value = temp
        return result
    }

    fun isSearchQueryStackEmpty(): Boolean {
        return searchQueryStack.value!!.isEmpty()
    }

    fun getPeekElementFromSearchQueryStack(): String {
        return searchQueryStack.value!!.peek()
    }

    fun clearSearchQueryStack() {
        val temp = searchQueryStack.value
        temp!!.clear()
        searchQueryStack.value = temp
    }

    private suspend fun safeSearchNewsCall(searchQuery: String, sortBy: String) {
        newSearchQuery = searchQuery
        searchNews.postValue(Resource.Loading())
        try {
            if(hasInternetConnection()) {
                searchNewsPage++
                val response = newsRepository.searchNews(searchNewsPage, searchQuery, sortBy)
                searchNews.postValue(handleSearchNewsResponse(response))
            } else {
                searchNews.postValue(Resource.Error("No internet connection"))
            }
        } catch(t: Throwable) {
            when(t) {
                is IOException -> searchNews.postValue(Resource.Error("Network Failure"))
                else -> searchNews.postValue(Resource.Error("Conversion Error"))
            }
        }
    }

    private suspend fun safeBreakingNewsCall(category: String, countryAbbr: String) {
        breakingNews.postValue(Resource.Loading())
        try {
            if(hasInternetConnection()) {
                breakingNewsPage++
                val response = newsRepository.getBreakingNews(breakingNewsPage, category, countryAbbr)
                breakingNews.postValue(handleBreakingNewsResponse(response))
            } else {
                breakingNews.postValue(Resource.Error("No internet connection"))
            }
        } catch(t: Throwable) {
            when(t) {
                is IOException -> breakingNews.postValue(Resource.Error("Network Failure"))
                else -> breakingNews.postValue(Resource.Error("Conversion Error"))
            }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = app.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB) -> true
            else -> false
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//        }
//        else {
//            connectivityManager.activeNetwork?.run {
//
//            }
//        }
//        return false
    }
}