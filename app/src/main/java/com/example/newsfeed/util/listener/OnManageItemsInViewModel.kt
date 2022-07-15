package com.example.newsfeed.util.listener

import com.example.newsfeed.entity.Article

interface OnManageItemsInViewModel {

    fun addSelectedItemToList(article: Article)

    fun removeUnselectedItemFromList(article: Article)

    fun addSelectedItemPositionToList(position: Int)

    fun removeUnselectedPositionFromList(position: Int)

}