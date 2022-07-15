package com.example.newsfeed.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newsfeed.repository.NewsRepository

class NewsViewModelFactory(
    val application: Application,
    val repository: NewsRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            return NewsViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }

}