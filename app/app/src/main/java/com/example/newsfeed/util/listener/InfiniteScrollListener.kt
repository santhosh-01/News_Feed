package com.example.newsfeed.util.listener

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InfiniteScrollListener(private val linearLayoutManager: LinearLayoutManager, private val listener: OnLoadMoreListener) : RecyclerView.OnScrollListener() {

    private val VISIBLE_THRESHOLD = 2
    private var loading = false // LOAD MORE Progress dialog
    private var pauseListening = false

    private var END_OF_FEED_ADDED = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (dx == 0 && dy == 0) return
        val totalItemCount = linearLayoutManager.itemCount
        val lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
        if (!loading && totalItemCount <= lastVisibleItem + VISIBLE_THRESHOLD && totalItemCount != 0 && !END_OF_FEED_ADDED && !pauseListening) {
            listener.onLoadMore()
            loading = true
        }
    }

    fun setLoaded() {
        loading = false
    }

    fun addEndOfRequests() {
        END_OF_FEED_ADDED = true
    }

    interface OnLoadMoreListener {
        fun onLoadMore()
    }

    fun pauseScrollListener(pauseListening: Boolean) {
        this.pauseListening = pauseListening
    }
}