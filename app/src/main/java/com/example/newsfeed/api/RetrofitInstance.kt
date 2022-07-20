package com.example.newsfeed.api

import com.example.newsfeed.util.Constants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {

    companion object {

        //Creating Retrofit Instance
        private val retrofit by lazy {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client = OkHttpClient.Builder().addInterceptor(loggingInterceptor).build()
            Retrofit.Builder().baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()).client(client).build()
        }

        //Creating the public variable for api using NewsAPI Interface
        val api: NewsAPI by lazy {
            retrofit.create(NewsAPI::class.java)
        }

    }

}