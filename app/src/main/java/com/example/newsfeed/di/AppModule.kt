package com.example.newsfeed.di

import android.app.Application
import com.example.newsfeed.db.ArticleDatabase
import com.example.newsfeed.repository.NewsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Singleton
    @Provides
    fun provideArticleDatabase(app: Application): ArticleDatabase {
        return ArticleDatabase.getInstance(app)
    }

    @Singleton
    @Provides
    fun provideNewsRepository(db: ArticleDatabase): NewsRepository {
        return NewsRepository(db)
    }

}