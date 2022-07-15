package com.example.newsfeed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.newsfeed.R
import com.example.newsfeed.entity.Article
import com.example.newsfeed.home.OnArticleClickListener
import com.example.newsfeed.ui.fragments.OnManageItemsInViewModel
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NewsAdapter(
    private val onArticleClickListener: OnArticleClickListener,
    private val onManageItemsInViewModel: OnManageItemsInViewModel,
    private var articleList: MutableList<Article?>
) : RecyclerView.Adapter<NewsAdapter.CustomViewHolder>() {

    private val VIEW_TYPE_ITEM: Int = 1
    private val VIEW_TYPE_LOADING: Int = 0
    var isCheckboxEnabled = false
    var isHomePage = true
    var canSelectBookmark = false
//    private val selectedNewsArticlesPosition: MutableList<Int> = mutableListOf()

//    val selectedItemPositions: MutableLiveData<List<Int>> = MutableLiveData(listOf())

    inner class ArticleViewHolder(itemView: View) : CustomViewHolder(itemView) {

        val secondPart: LinearLayout
        val textTitle: TextView
        val textSource: TextView
        val textTime: TextView
        val progressBar: ProgressBar
        val newsImage: ImageView
        val cardView: MaterialCardView
        val shareButton: ImageButton
        val bookmarkButton: ToggleButton
        val checkBox: CheckBox
        val blockedCheckBox: View

        init {
            secondPart = itemView.findViewById(R.id.secondPart)
            textTitle = itemView.findViewById(R.id.text_title)
            textSource = itemView.findViewById(R.id.text_source)
            textTime = itemView.findViewById(R.id.text_time)
            progressBar = itemView.findViewById(R.id.progressBar)
            newsImage = itemView.findViewById(R.id.news_image)
            cardView = itemView.findViewById(R.id.container)
            bookmarkButton = itemView.findViewById(R.id.bookmark_toggle)
            shareButton = itemView.findViewById(R.id.share_button)
            checkBox = itemView.findViewById(R.id.article_check_box)
            blockedCheckBox = itemView.findViewById(R.id.blocked_checkbox)
        }

        private fun initUIElements(currentArticle: Article, position: Int) {
            initTextElements(currentArticle)
            initImageElement(currentArticle)
            initClickListeners(currentArticle, position)
            bookmarkButton.isChecked = currentArticle.isExistInDB
            checkBox.isChecked = currentArticle.isChecked
        }

        private fun initClickListeners(currentArticle: Article, position: Int) {
            cardView.setOnClickListener {
                onArticleClickListener.onClick(currentArticle)
            }
            bookmarkButton.setOnClickListener {
                onArticleClickListener.onBookmarkButtonClick(currentArticle)
                notifyItemChanged(position)
            }
            shareButton.setOnClickListener {
                onArticleClickListener.onShareButtonClick(currentArticle)
            }
            if ((isHomePage && !isCheckboxEnabled && !currentArticle.isExistInDB) || (!isHomePage && !isCheckboxEnabled && currentArticle.isExistInDB)) {
                cardView.setOnLongClickListener {
                    onArticleClickListener.onLongClick(currentArticle, cardView)
                    isCheckboxEnabled = true
                    checkBox.isChecked = true
                    currentArticle.isChecked = true
                    onManageItemsInViewModel.addSelectedItemPositionToList(position)
                    onManageItemsInViewModel.addSelectedItemToList(currentArticle)
                    notifyDataSetChanged()
                    true
                }
            } else {
                cardView.setOnLongClickListener {
                    onArticleClickListener.onLongClick(currentArticle, cardView)
                    true
                }
            }
            if (!isCheckboxEnabled) {
                checkBox.visibility = View.GONE
                secondPart.visibility = View.VISIBLE
                blockedCheckBox.visibility = View.GONE
            }
            if (isCheckboxEnabled) {
                secondPart.visibility = View.GONE
                checkBox.visibility = View.VISIBLE
                if ((isHomePage && currentArticle.isExistInDB) || (!isHomePage && !currentArticle.isExistInDB)) {
//                    checkBox.isChecked = false
//                    cardView.isEnabled = false
//                    cardView.isClickable = false
                    blockedCheckBox.visibility = View.VISIBLE
                    cardView.setOnClickListener(null)
                } else {
                    blockedCheckBox.visibility = View.GONE
                    cardView.setOnClickListener {
                        if (!checkBox.isChecked) {
                            onManageItemsInViewModel.addSelectedItemPositionToList(position)
                            onManageItemsInViewModel.addSelectedItemToList(currentArticle)
                            checkBox.isChecked = true
                            currentArticle.isChecked = true
                        } else {
                            onManageItemsInViewModel.removeUnselectedPositionFromList(position)
                            onManageItemsInViewModel.removeUnselectedItemFromList(currentArticle)
                            checkBox.isChecked = false
                            currentArticle.isChecked = false
                        }
                    }
                }
                cardView.setOnLongClickListener(null)
            }
            /*checkBox.setOnClickListener {
                if (checkBox.isChecked) {
                    onManageItemsInViewModel.addSelectedItemPositionToList(position)
                    onManageItemsInViewModel.addSelectedItemToList(currentArticle)
                    checkBox.isChecked = true
                }
                else {
                    onManageItemsInViewModel.removeUnselectedPositionFromList(position)
                    onManageItemsInViewModel.removeUnselectedItemFromList(currentArticle)
                    checkBox.isChecked = false
                }
            }*/
        }

        private fun initImageElement(currentArticle: Article) {
            if (!currentArticle.urlToImage.isNullOrBlank()) {
                Picasso.get().load(currentArticle.urlToImage).into(newsImage, object : Callback {
                    override fun onSuccess() {
                        progressBar.visibility = View.GONE
                    }

                    override fun onError(e: Exception?) {
                        newsImage.setImageResource(R.drawable.img_not_available)
                        progressBar.visibility = View.GONE
                    }
                })
            } else {
                newsImage.setImageResource(R.drawable.img_not_available)
                progressBar.visibility = View.GONE
            }
        }

        private fun initTextElements(currentArticle: Article) {
            textTitle.text = currentArticle.title
            textSource.text = currentArticle.source?.name ?: "Not Found"
            textTime.text = parseTime(currentArticle.publishedAt)
        }

        private fun parseTime(publishedTime: String?): String {
            val (date, time) = publishedTime?.split("T") ?: listOf("-1", "-1")
            if (date == "-1" || time == "-1") return "Not Found"

            // Find Today's Date
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val formatted = current.format(formatter)

            val result = if (formatted == date)
                "Today, "
            else
                "$date, "

            val timeResult = time.dropLast(4)
//                .split(":").map { it.toInt() }
//            val timeResult = if (hours == 0)
//                "12:${minutes.formatToDoubleDigit()} AM"
//            else if (hours == 12)
//                "${hours}:${minutes.formatToDoubleDigit()} PM"
//            else if (hours > 12)
//                "${(hours % 12).formatToDoubleDigit()}:${minutes.formatToDoubleDigit()} PM"
//            else
//                "${hours.formatToDoubleDigit()}:${minutes.formatToDoubleDigit()} AM"

            return "$result $timeResult UTC"
        }

        // Extension Function to get Double Digit using the integer
        private fun Int.formatToDoubleDigit(): String {
            return "%02d".format(this)
        }

        fun bind(position: Int) {
            val currentArticle = articleList[position]

            currentArticle?.let {
                initUIElements(currentArticle, position)
            }
//            cardView.isChecked = currentArticle.isChecked
//            textTitle.text = currentArticle.title
//            textSource.text = currentArticle.source?.name ?: "Not Found"
//            textTime.text = currentArticle.publishedAt ?: ""
//
//            if (!currentArticle.urlToImage.isNullOrBlank()) {
//                Picasso.get().load(currentArticle.urlToImage).into(newsImage,object : Callback {
//                    override fun onSuccess() {
//                        progressBar.visibility = View.GONE
//                    }
//
//                    override fun onError(e: Exception?) {
//                        newsImage.setImageResource(R.drawable.img_not_available)
//                        progressBar.visibility = View.GONE
//                    }
//                })
//            }
//            else {
//                newsImage.setImageResource(R.drawable.img_not_available)
//                progressBar.visibility = View.GONE
//            }
//
//            bookmarkButton.isChecked = currentArticle.isExistInDB
//
//            cardView.setOnClickListener {
//                onArticleClickListener.onClick(articleList[adapterPosition])
//            }
//
//            bookmarkButton.setOnClickListener {
//                onArticleClickListener.onBookmarkButtonClick(articleList[adapterPosition])
//            }
//
//            shareButton.setOnClickListener {
//                onArticleClickListener.onShareButtonClick(articleList[adapterPosition])
//            }
//
//            if (isCheckboxEnabled) {
//                checkBox.visibility = View.VISIBLE
//                cardView.setOnLongClickListener(null)
//            }
//            else {
//                checkBox.visibility = View.GONE
//
//                cardView.setOnLongClickListener { cardView ->
//                    val card = cardView as MaterialCardView
//                    onArticleClickListener.onLongClick(articleList[adapterPosition], card)
////                if (card.isCheckable) {
////                    if (!articleList[adapterPosition].isChecked) {
////                        onManageItemsInViewModel.addSelectedItemToList(articleList[adapterPosition])
////                        onManageItemsInViewModel.addSelectedItemPositionToList(adapterPosition)
////                        cardView.isChecked = true
////                        articleList[adapterPosition].isChecked = true
////                    }
////                    else {
////                        onManageItemsInViewModel.removeUnselectedItemFromList(articleList[adapterPosition])
////                        onManageItemsInViewModel.removeUnselectedPositionFromList(adapterPosition)
////                        cardView.isChecked = false
////                        articleList[adapterPosition].isChecked = false
////                    }
////                }
//                    isCheckboxEnabled = true
//                    checkBox.isChecked = true
//                    notifyDataSetChanged()
//                    true
//                }
//            }
//
//            checkBox.isEnabled = !articleList[adapterPosition].isExistInDB
//
//            checkBox.setOnClickListener {
//                if (checkBox.isChecked) {
//                    onManageItemsInViewModel.addSelectedItemToList(articleList[adapterPosition])
//                    onManageItemsInViewModel.addSelectedItemPositionToList(adapterPosition)
////                    articleList[adapterPosition].isChecked = true
//                }
//                else {
//                    onManageItemsInViewModel.removeUnselectedItemFromList(articleList[adapterPosition])
//                    onManageItemsInViewModel.removeUnselectedPositionFromList(adapterPosition)
////                    articleList[adapterPosition].isChecked = false
//                }
//            }
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

    inner class ProgressViewHolder(itemView: View) : CustomViewHolder(itemView) {

    }

    open inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val root =
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.news_article_item,
                        parent,
                        false
                    )
            ArticleViewHolder(root)
        } else {
            val root =
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_progress,
                        parent,
                        false
                    )
            ProgressViewHolder(root)
        }

//        return ArticleViewHolder(
//            LayoutInflater.from(parent.context).inflate(R.layout.news_article_item, parent, false)
//        )
    }

    override fun getItemCount(): Int {
        return articleList.size
    }

//    override fun onBindViewHolder(
//        holder: ArticleViewHolder,
//        position: Int
//    ) {
//        holder.bind(position)
//    }

    fun loadList(list: List<Article>) {
        articleList.clear()
        list.forEach {
            articleList.add(it)
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        if (holder is ArticleViewHolder)
            holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int {
        if (articleList[position] != null)
            return VIEW_TYPE_ITEM;
        else
            return VIEW_TYPE_LOADING;
    }

    fun addNullData() {
        articleList.add(null)
        notifyItemInserted(articleList.size - 1)
    }

    fun removeNull() {
        if (articleList.isNotEmpty()) {
            articleList.removeAt(articleList.size - 1)
            notifyItemRemoved(articleList.size)
        }
    }

    fun addData(integersList: MutableList<Article>) {
        articleList.clear()
        integersList.forEach {
            articleList.add(it)
        }
        notifyDataSetChanged()
    }
}