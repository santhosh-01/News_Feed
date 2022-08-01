package com.example.newsfeed.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsfeed.entity.Article
import com.example.newsfeed.entity.NewsArticle
import com.example.newsfeed.repository.NewsRepository
import com.example.newsfeed.util.Constants.Companion.API_KEYS
import com.example.newsfeed.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import retrofit2.Response
import java.io.IOException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    val app: Application,
    val newsRepository: NewsRepository
) : ViewModel() {

    private val sharedPref: SharedPreferences =
        app.getSharedPreferences("application", Context.MODE_PRIVATE)

    var apiKey: String = API_KEYS[0]

    var sortBy: String = "relevancy"
    val languageMap: HashMap<String, String> = hashMapOf()
    val countryMap: HashMap<String, String> = hashMapOf()

//    private var apiKey: String = API_KEY

//    var isSearchOn: Boolean = false
//    var breakingNewsAdapter: NewsAdapter? = null

    var selectedCategory: String = ""
    var selectedCountry: String = ""
    var preferredLanguage: String = "en"

    var breakingNewsResponse: NewsArticle? = null
    var searchNewsResponse: NewsArticle? = null

    var breakingNewsPage: Int = 1
    var searchNewsPage: Int = 1

    var searchQuery: String? = null
    var oldSearchQuery: String? = null

    var bookmarkSearchQuery: String? = null

    val searchQueryStack: MutableLiveData<Stack<String>> = MutableLiveData(Stack())

    val articleList: MutableList<Article> = mutableListOf()
    var temp: MutableList<Article> = mutableListOf()

    val breakingNews: MutableLiveData<Resource<NewsArticle>> = MutableLiveData()

    val searchNews: MutableLiveData<Resource<NewsArticle>> = MutableLiveData()

    val selectedNewsListInBookmarks: MutableList<Article> = mutableListOf()
    val selectedNewsListInHome: MutableList<Article> = mutableListOf()

    val selectedItemPositionsInHome: MutableLiveData<List<Int>> = MutableLiveData(listOf())
    private val selectedItemPositionsInBookmark: MutableLiveData<List<Int>> =
        MutableLiveData(listOf())

    init {
        initCategoryAndCountryForFirstTime()
        setupMapOfLanguages()
        setupMapOfCountries()
    }

    private fun setupMapOfCountries() {
        val countries = app.resources.getStringArray(com.example.newsfeed.R.array.country_code_array)
        for (countryNameWithCountryAbbr in countries) {
            val (countryName, countryAbbr) = countryNameWithCountryAbbr.split(" - ")
            countryMap[countryAbbr] = countryName
        }
    }

    private fun initCategoryAndCountryForFirstTime() {
        if (!sharedPref.contains("category")) {
            saveNewsCategory("general")
        }

        if (!sharedPref.contains("country")) {
            saveNewsCountry("global")
        }
    }

    private fun setupMapOfLanguages() {
        val languages = app.resources.getStringArray(com.example.newsfeed.R.array.language_code_array)

        for (languageNameWithLanguageAbbr in languages) {
            val (languageName, languageAbbr) = languageNameWithLanguageAbbr.split(" - ")
            languageMap[languageAbbr.lowercase()] = languageName
        }
    }

    fun initBreakingNews() {
        getBreakingNews(
            sharedPref.getString("category", "")!!,
            sharedPref.getString("country", "")!!
        )
    }

    fun initAccordingToCurrentConfig() {
        val currentCategory = sharedPref.getString("category", "")
        val currentCountry = sharedPref.getString("country", "")
        selectedCategory = currentCategory!!
        selectedCountry = currentCountry!!
        initBreakingNews()
    }

    fun initSearchAccordingToCurrentConfig() {
        searchQuery?.let {
            searchNewsPage = 1
            searchNewsResponse = null
            searchNews(searchQuery!!, sortBy)
        }
    }

    fun getBreakingNews(category: String, countryAbbr: String) = viewModelScope.launch {
//        breakingNews.postValue(Resource.Loading())
//        val response: Response<NewsArticle> = newsRepository.getBreakingNews(breakingNewsPage, category, countryAbbr)
//        breakingNews.postValue(handleBreakingNewsResponse(response))

        safeBreakingNewsCall(category, countryAbbr)
    }

    fun searchNews(searchQuery: String, sortBy: String = "") = viewModelScope.launch {
//        searchNews.postValue(Resource.Loading())
//        val response: Response<NewsArticle> = newsRepository.searchNews(searchQuery)
//        searchNews.postValue(handleSearchNewsResponse(response))

        this@NewsViewModel.searchQuery = searchQuery
        this@NewsViewModel.sortBy = sortBy

        safeSearchNewsCall(searchQuery, sortBy, preferredLanguage)
    }

    private fun handleBreakingNewsResponse(response: Response<NewsArticle>): Resource<NewsArticle> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                if (breakingNewsResponse == null) {
                    breakingNewsResponse = resultResponse
                } else {
                    val oldArticles = breakingNewsResponse?.articles
                    val newArticles = resultResponse.articles

                    oldArticles?.addAll(newArticles)
                }
                breakingNewsPage++
                return Resource.Success(breakingNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error("Request unsuccessful")
    }

    fun getArticleFromViewModelByTitle(title: String): Article? {
        if (!isSearchQueryStackEmpty()) {
            searchNewsResponse!!.articles.forEach {
                if (it.title == title) return it
            }
        }
        else {
            breakingNewsResponse!!.articles.forEach {
                if (it.title == title) return it
            }
        }
        return null
    }

    fun getSavedArticleFromViewModelByTitle(title: String): Article? {
        articleList.forEach {
            if (it.title == title) return it
        }
        return null
    }

    private fun handleSearchNewsResponse(response: Response<NewsArticle>): Resource<NewsArticle> {
        if (response.isSuccessful) {
            response.body()?.let { resultResponse ->
                Log.i("NewsViewModel", response.body().toString())
                if (response.body()?.totalResults == 0) {
                    return Resource.Error("No Result Found!!")
                }
                if (searchNewsResponse == null) {
                    searchNewsResponse = resultResponse
                } else {
                    val oldArticles = searchNewsResponse?.articles
                    val newArticles = resultResponse.articles

                    oldArticles?.addAll(newArticles)
                }
                searchNewsPage++
                return Resource.Success(searchNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error("Response unsuccessful")
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
        selectedItemPositionsInBookmark.value =
            selectedItemPositionsInBookmark.value!!.plus(position)
    }

    fun removeUnselectedItemPositionFromBookmark(position: Int) {
        selectedItemPositionsInBookmark.value =
            selectedItemPositionsInBookmark.value!!.minus(position)
    }

    private fun clearSelectedItemsInHome() {
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

    fun unSelectBookmarkArticles() {
        selectedNewsListInBookmarks.forEach {
            it.isChecked = false
        }
        clearSelectedItemsInBookmark()
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

    private suspend fun safeSearchNewsCall(
        searchQuery: String,
        sortBy: String,
        language: String
    ) {
        this.searchQuery = searchQuery
        searchNews.postValue(Resource.Loading())
        try {
            if (hasInternetConnection()) {
                val response =
                    newsRepository.searchNews(searchNewsPage, searchQuery, sortBy, language, apiKey)
                searchNews.postValue(handleSearchNewsResponse(response))
            } else {
                searchNews.postValue(Resource.Error("No internet connection",searchNewsResponse))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> searchNews.postValue(Resource.Error("Network Failure"))
                else -> searchNews.postValue(Resource.Error("Conversion Error"))
            }
        }
    }

    private suspend fun safeBreakingNewsCall(
        category: String,
        countryAbbr: String
    ) {
        breakingNews.postValue(Resource.Loading())
        try {
            if (hasInternetConnection()) {
                val response =
                    newsRepository.getBreakingNews(breakingNewsPage, category, countryAbbr, apiKey)
                breakingNews.postValue(handleBreakingNewsResponse(response))
            } else {
                breakingNews.postValue(Resource.Error("No internet connection", breakingNewsResponse))
            }
        } catch (t: Throwable) {
            when (t) {
                is IOException -> breakingNews.postValue(Resource.Error("Network Failure"))
                else -> breakingNews.postValue(Resource.Error("Conversion Error"))
            }
        }
    }

    fun hasInternetConnection(): Boolean {
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

    suspend fun changeAPIKey(): Boolean {
        Log.i("NewsViewModel",Thread.currentThread().name.toString()+"4")
        val successFlag = withContext(Dispatchers.IO) {
            var successFlag = false
            Log.i("NewsViewModel",Thread.currentThread().name.toString()+"5")
            for (apiKey in API_KEYS.drop(1))  {
                this@NewsViewModel.apiKey = apiKey
                val response =
                    newsRepository.getBreakingNews(breakingNewsPage, selectedCategory, selectedCountry, apiKey)
                if (response.isSuccessful) {
                    getBreakingNews(selectedCategory, selectedCountry)
                    successFlag = true
                    break
                }
            }
            return@withContext successFlag
        }
        Log.i("NewsViewModel",Thread.currentThread().name.toString()+"6")
        return successFlag
    }

    fun changeAPIKeyForSearch(): Boolean {
        var successFlag = false
        runBlocking {
            val task = async {
                for (apiKey in API_KEYS.drop(1))  {
                    this@NewsViewModel.apiKey = apiKey
                    val response =  newsRepository.searchNews(
                        searchNewsPage,
                        searchQuery!!,
                        sortBy,
                        preferredLanguage,
                        apiKey
                    )
                    if (response.isSuccessful) {
                        searchNews(searchQuery!!, sortBy)
                        successFlag = true
                        Log.i("NewsViewModel","API SUCCESS")
                        break
                    }
                }
            }
            task.await()
        }
        return successFlag
    }

    fun filterBookmarkNews(query: String): MutableList<Article> {
        if (bookmarkSearchQuery == null)
            temp.addAll(articleList)
        bookmarkSearchQuery = query
        val filteredList: MutableList<Article> = mutableListOf()
        for (article in articleList) {
            if (article.title!!.lowercase().contains(query.lowercase())) {
                filteredList.add(article)
            }
        }
        return filteredList
    }

    fun revertFilterBookmark() {
        articleList.clear()
        articleList.addAll(temp)
        bookmarkSearchQuery = null
//        temp.clear()
    }

    fun saveNewsCategory(category: String) {
        val editor = sharedPref.edit()
        editor.putString("category", category)
        editor.apply()
    }

    fun saveNewsCountry(countryAbbr: String) {
        val editor = sharedPref.edit()
        editor.putString("country", countryAbbr)
        editor.apply()
    }

    fun getNewsCategoryFromSharedPref(): String {
        return sharedPref.getString("category","")!!
    }

    fun isCategoryOrCountryChanged(): Boolean {
        val currentCategory = sharedPref.getString("category", "")
        val currentCountry = sharedPref.getString("country", "")

        return selectedCategory != currentCategory || selectedCountry != currentCountry
    }

    fun changeCategoryAndCountryOnViewModel() {
        val currentCategory = sharedPref.getString("category", "")
        val currentCountry = sharedPref.getString("country", "")

        selectedCategory = currentCategory!!
        selectedCountry = currentCountry!!
    }

    fun clearAdapterAndGetBreakingNews() {
        if (hasInternetConnection()) {
            breakingNewsPage = 1
            breakingNewsResponse = null
        }
        initBreakingNews()
    }

    fun clearListAndGetSearchNews(searchQuery: String) {
        searchNewsPage = 1
        searchNewsResponse = null
        pushToSearchQueryStack(searchQuery)
        searchNews(searchQuery, sortBy)
    }
}