package com.example.newsfeed.util.listener

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class InfiniteScrollListener(private val linearLayoutManager: LinearLayoutManager, private val listener: OnLoadMoreListener) : RecyclerView.OnScrollListener() {

    companion object {
        private const val VISIBLE_THRESHOLD = 2
    }

    private var loading = false // LOAD MORE Progress dialog
    private var pauseListening = false

    private var endOfFeedAdded = false

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        if (dx == 0 && dy == 0) return
        val totalItemCount = linearLayoutManager.itemCount
        val lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
        if (!loading && totalItemCount <= lastVisibleItem + VISIBLE_THRESHOLD && totalItemCount != 0 && !endOfFeedAdded && !pauseListening) {
            listener.onLoadMore()
            loading = true
        }
    }

    fun setLoaded() {
        loading = false
    }

    fun addEndOfRequests() {
        endOfFeedAdded = true
    }

    interface OnLoadMoreListener {
        fun onLoadMore()
    }

    fun pauseScrollListener(pauseListening: Boolean) {
        this.pauseListening = pauseListening
    }
}