package com.example.newsfeed.adapter

import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.newsfeed.R
import com.example.newsfeed.entity.Article
import com.example.newsfeed.home.OnArticleClickListener
import com.example.newsfeed.ui.fragments.OnManageItemsInViewModel
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.internal.notifyAll

class NewsAdapter(
    private val onArticleClickListener: OnArticleClickListener,
    private val onManageItemsInViewModel: OnManageItemsInViewModel,
    private var articleList: MutableList<Article>
): RecyclerView.Adapter<NewsAdapter.ArticleViewHolder>() {

//    private var itemStateArray: SparseBooleanArray = SparseBooleanArray()

//    val selectedItemPositions: MutableLiveData<List<Int>> = MutableLiveData(listOf())

    inner class ArticleViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        val textTitle: TextView
        val textSource: TextView
        val textTime: TextView
        val progressBar: ProgressBar
        val newsImage: ImageView
        val cardView: MaterialCardView
        val shareButton: ImageButton
        val bookmarkButton: ToggleButton

        init {
            textTitle = itemView.findViewById(R.id.text_title)
            textSource = itemView.findViewById(R.id.text_source)
            textTime = itemView.findViewById(R.id.text_time)
            progressBar = itemView.findViewById(R.id.progressBar)
            newsImage = itemView.findViewById(R.id.news_image)
            cardView = itemView.findViewById(R.id.container)
            shareButton = itemView.findViewById(R.id.share_button)

            bookmarkButton = itemView.findViewById(R.id.bookmark_toggle)
        }

        fun bind(position: Int) {
            val currentArticle = articleList[position]
            cardView.isChecked = currentArticle.isChecked
            textTitle.text = currentArticle.title
            textSource.text = currentArticle.source?.name ?: "Not Found"
            textTime.text = currentArticle.publishedAt ?: ""

            if (!currentArticle.urlToImage.isNullOrBlank()) {
                Picasso.get().load(currentArticle.urlToImage).into(newsImage,object : Callback {
                    override fun onSuccess() {
                        progressBar.visibility = View.GONE
                    }

                    override fun onError(e: Exception?) {
                        newsImage.setImageResource(R.drawable.img_not_available)
                        progressBar.visibility = View.GONE
                    }
                })
            }
            else {
                newsImage.setImageResource(R.drawable.img_not_available)
                progressBar.visibility = View.GONE
            }

            bookmarkButton.isChecked = currentArticle.isExistInDB

            cardView.setOnClickListener {
                onArticleClickListener.onClick(articleList[adapterPosition])
            }

            cardView.setOnLongClickListener { cardView ->
                val card = cardView as MaterialCardView
                onArticleClickListener.onLongClick(articleList[adapterPosition], card)
                if (card.isCheckable) {
                    if (!articleList[adapterPosition].isChecked) {
                        onManageItemsInViewModel.addSelectedItemToList(articleList[adapterPosition])
                        onManageItemsInViewModel.addSelectedItemPositionToList(adapterPosition)
                        cardView.isChecked = true
                        articleList[adapterPosition].isChecked = true
                    }
                    else {
                        onManageItemsInViewModel.removeUnselectedItemFromList(articleList[adapterPosition])
                        onManageItemsInViewModel.removeUnselectedPositionFromList(adapterPosition)
                        cardView.isChecked = false
                        articleList[adapterPosition].isChecked = false
                    }
                }
                true
            }

            bookmarkButton.setOnClickListener {
                onArticleClickListener.onBookmarkButtonClick(articleList[adapterPosition])
            }

            shareButton.setOnClickListener {
                onArticleClickListener.onShareButtonClick(articleList[adapterPosition])
            }
        }

        /*override fun onLongClick(v: View?): Boolean {
            onArticleClickListener.onLongClick(articleList[adapterPosition])
            val card = v as MaterialCardView
            if (card.isCheckable) {
                if (!articleList[adapterPosition].isChecked) {
                    onManageItemsInViewModel.addSelectedItemToList(articleList[adapterPosition])
                    onManageItemsInViewModel.addSelectedItemPositionToList(adapterPosition)
                    cardView.isChecked = true
                    articleList[adapterPosition].isChecked = true
                }
                else {
                    onManageItemsInViewModel.removeUnselectedItemFromList(articleList[adapterPosition])
                    onManageItemsInViewModel.removeUnselectedPositionFromList(adapterPosition)
                    cardView.isChecked = false
                    articleList[adapterPosition].isChecked = false
                }
            }
            return true
        }

        override fun onClick(v: View?) {
            if (v!!.id == R.id.bookmark_toggle) {
//                bookmarkButton.isChecked = !bookmarkButton.isChecked
                onArticleClickListener.onBookmarkButtonClick(articleList[adapterPosition])
//                notifyItemChanged(adapterPosition)
            }
            else
                onArticleClickListener.onClick(articleList[adapterPosition])
        }*/

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        return ArticleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.news_article_item,parent,false))
    }

    override fun getItemCount(): Int {
        return articleList.size
    }

    override fun onBindViewHolder(
        holder: ArticleViewHolder,
        position: Int
    ) {
        holder.bind(position)
    }

    fun loadList(list: List<Article>) {
        articleList.clear()
        list.forEach {
            articleList.add(it)
        }
        notifyDataSetChanged()
    }

}