package com.example.newsfeed.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.newsfeed.R
import com.example.newsfeed.entity.Article
import com.example.newsfeed.util.listener.OnArticleClickListener
import com.example.newsfeed.util.listener.OnManageItemsInViewModel
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import java.lang.Exception

class NewsAdapter(
    private val onArticleClickListener: OnArticleClickListener,
    private val onManageItemsInViewModel: OnManageItemsInViewModel,
    private var articleList: MutableList<Article?>
) : RecyclerView.Adapter<NewsAdapter.CustomViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM: Int = 1
        private const val VIEW_TYPE_LOADING: Int = 0
    }

    var isCheckboxEnabled = false
    var isHomePage = true
    var canSelectBookmark = false

    private lateinit var shimmerDrawable: ShimmerDrawable

//    private val selectedNewsArticlesPosition: MutableList<Int> = mutableListOf()

//    val selectedItemPositions: MutableLiveData<List<Int>> = MutableLiveData(listOf())

    inner class ArticleViewHolder(itemView: View) : CustomViewHolder(itemView) {

        private val secondPart: LinearLayout
        private val textTitle: TextView
        private val textSource: TextView
        private val textTime: TextView
//        val progressBar: ProgressBar
        val newsImage: ImageView
        private val cardView: MaterialCardView
        private val shareButton: ImageButton
        private val bookmarkButton: ToggleButton
        private val checkBox: CheckBox
        private val blockedCheckBox: View

        init {
            secondPart = itemView.findViewById(R.id.secondPart)
            textTitle = itemView.findViewById(R.id.text_title)
            textSource = itemView.findViewById(R.id.text_source)
            textTime = itemView.findViewById(R.id.text_time)
//            progressBar = itemView.findViewById(R.id.progressBar)
            newsImage = itemView.findViewById(R.id.news_image)
            cardView = itemView.findViewById(R.id.card_container)
            bookmarkButton = itemView.findViewById(R.id.bookmark_toggle)
            shareButton = itemView.findViewById(R.id.share_button)
            checkBox = itemView.findViewById(R.id.article_check_box)
            blockedCheckBox = itemView.findViewById(R.id.blocked_checkbox)
        }

        private fun initUIElements(currentArticle: Article, position: Int) {
            initUIElements(currentArticle)
            initClickListeners(currentArticle, position)
            bookmarkButton.isChecked = currentArticle.isExistInDB
            checkBox.isChecked = currentArticle.isChecked
        }

        private fun initClickListeners(currentArticle: Article, position: Int) {
            secondPart.setOnClickListener(null)
            if (!isCheckboxEnabled) {
                secondPart.visibility = View.VISIBLE
                checkBox.visibility = View.GONE
                blockedCheckBox.visibility = View.GONE
            }
            else {
                checkBox.visibility = View.VISIBLE
                secondPart.visibility = View.GONE
                blockedCheckBox.isVisible = !currentArticle.isCheckable
            }

            cardView.setOnClickListener {
                if (!isCheckboxEnabled) {
                    onArticleClickListener.onClick(currentArticle)
                }
                else {
                    if (currentArticle.isCheckable) {
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
            }
            bookmarkButton.setOnClickListener {
                onArticleClickListener.onBookmarkButtonClick(currentArticle)
                notifyItemChanged(position)
            }
            shareButton.setOnClickListener {
                onArticleClickListener.onShareButtonClick(currentArticle)
            }
            cardView.setOnLongClickListener {
                if (currentArticle.isCheckable) {
                    onArticleClickListener.onLongClick(currentArticle, cardView)
//                    checkBox.isChecked = true
                    currentArticle.isChecked = true
                    onManageItemsInViewModel.addSelectedItemPositionToList(position)
                    onManageItemsInViewModel.addSelectedItemToList(currentArticle)
                    isCheckboxEnabled = true
                    notifyDataSetChanged()
                    true
                }
                else {
                    onArticleClickListener.onLongClick(currentArticle, cardView)
                    true
                }
            }
        }

        private fun initUIElements(currentArticle: Article) {
            if (!currentArticle.urlToImage.isNullOrBlank()) {
                Picasso.get().load(currentArticle.urlToImage).placeholder(shimmerDrawable).into(newsImage, object : Callback {
                    override fun onSuccess() {
                    }

                    override fun onError(e: Exception?) {
                        newsImage.setImageResource(R.drawable.news_logo_final)
                    }

                })
            } else {
                newsImage.setImageResource(R.drawable.news_logo_final)
            }

            initTextElements(currentArticle)
        }

        private fun initTextElements(currentArticle: Article) {
            textTitle.text = currentArticle.title
            textSource.text = currentArticle.source?.name ?: "Not Found"
            textTime.text = parseTime(currentArticle.publishedAt)

            currentArticle.isCheckable = (isHomePage && !currentArticle.isExistInDB) || (!isHomePage && currentArticle.isExistInDB)
        }

        private fun parseTime(publishedTime: String?): String {
            val (date, time) = publishedTime?.split("T") ?: listOf("-1", "-1")
            if (date == "-1" || time == "-1") return "Not Found"

            // Find Today's Date
//            val current = LocalDateTime.now()
//            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//            val formatted = current.format(formatter)
//
//            val result = if (formatted == date)
//                "Today, "
//            else
//                "$date, "

            val result = "$date, "

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
//        private fun Int.formatToDoubleDigit(): String {
//            return "%02d".format(this)
//        }

        fun bind(position: Int) {
            val currentArticle = articleList[position]

            currentArticle?.let {
                initUIElements(currentArticle, position)
            }

        }

    }

    inner class ProgressViewHolder(itemView: View) : CustomViewHolder(itemView)

    open inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val root =
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.news_article_item1,
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

    fun loadList(list: List<Article>) {
        articleList.clear()
        list.forEach {
            articleList.add(it)
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val shimmer = Shimmer.ColorHighlightBuilder()
            .setBaseColor(Color.parseColor("#F3F3F3"))
            .setBaseAlpha(1.0F)
            .setHighlightColor(Color.parseColor("#BFBDBD"))
            .setHighlightAlpha(1.0F)
            .setDropoff(50.0F)
            .build()

        shimmerDrawable = ShimmerDrawable()
        shimmerDrawable.setShimmer(shimmer)

        if (holder is ArticleViewHolder)
            holder.bind(position)
    }

    override fun getItemViewType(position: Int): Int {
        return if (articleList[position] != null)
            VIEW_TYPE_ITEM
        else
            VIEW_TYPE_LOADING
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

    fun filterList(list: MutableList<Article>) {
        articleList = list.toMutableList()
        notifyDataSetChanged()
    }

    fun clearAdapterList() {
        articleList.clear()
    }
}