package com.example.newsfeed.ui.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsfeed.R
import com.example.newsfeed.adapter.NewsAdapter
import com.example.newsfeed.databinding.FragmentBookmarksBinding
import com.example.newsfeed.entity.Article
import com.example.newsfeed.viewmodel.NewsViewModel
import com.example.newsfeed.util.listener.OnArticleClickListener
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.util.listener.OnManageItemsInViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class BookmarksFragment : Fragment() {

    private lateinit var binding: FragmentBookmarksBinding
    private lateinit var viewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var overflowMenu: Menu

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        requireActivity().findViewById<SearchView>(R.id.search_view).visibility = View.GONE

        requireActivity().title = "Bookmarks"

        binding = FragmentBookmarksBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        setHasOptionsMenu(true)

//        newsAdapter = NewsAdapter(articleClickListener,onAddItemToList, listOf())
        setUpRecyclerView(mutableListOf())

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getSavedNewsArticles().observe(viewLifecycleOwner, Observer { articles ->
            viewModel.articleList.clear()
            viewModel.articleList.addAll(articles)
            if (articles.isNotEmpty()) {
                overflowMenu.findItem(R.id.delete_bookmark).isVisible = true
                articles.forEach { article ->
                    article.isExistInDB = true
                }
                newsAdapter.loadList(articles)
                binding.noBookmarkText.visibility = View.GONE
            }
            else {
                overflowMenu.findItem(R.id.delete_bookmark).isVisible = false
                newsAdapter.loadList(listOf())
                binding.noBookmarkText.visibility = View.VISIBLE
            }
        })
    }

    private fun unbookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.deleteArticle(viewModel.getArticleByTitle(article.title))
        }
        article.isExistInDB = false
    }

    private val articleClickListener = object : OnArticleClickListener {
        override fun onClick(article: Article) {
            val action = BookmarksFragmentDirections.actionBookmarksFragmentToArticlePreviewFragment(article, false)
            requireView().findNavController().navigate(action)
        }

        override fun onBookmarkButtonClick(article: Article) {
            unbookmarkArticle(article)
            val snackbar = Snackbar.make(requireView(), "Article removed from bookmarks successfully", Snackbar.LENGTH_SHORT)
            snackbar.setAction("Undo") {
                lifecycleScope.launch {
                    article.id = viewModel.insertArticle(article).toInt()
                }
                snackbar.dismiss()
            }
            snackbar.show()
            snackbar.view.setOnClickListener { snackbar.dismiss() }
            newsAdapter.notifyDataSetChanged()
        }

        override fun onShareButtonClick(article: Article) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, article.url)
            startActivity(shareIntent)
        }

        override fun onLongClick(article: Article, cardView: MaterialCardView) {
            cardView.isCheckable = true
        }

    }

    private val onManageItemsInViewModel: OnManageItemsInViewModel = object :
        OnManageItemsInViewModel {
        override fun addSelectedItemToList(article: Article) {
            viewModel.selectedNewsListInBookmarks.add(article)
        }

        override fun removeUnselectedItemFromList(article: Article) {
            viewModel.selectedNewsListInBookmarks.remove(article)
        }

        override fun addSelectedItemPositionToList(position: Int) {
            viewModel.addSelectedItemPositionInBookmark(position)
        }

        override fun removeUnselectedPositionFromList(position: Int) {
            viewModel.removeUnselectedItemPositionFromBookmark(position)
        }
    }

    private fun setUpRecyclerView(list: MutableList<Article?>) {
        newsAdapter = NewsAdapter(articleClickListener,onManageItemsInViewModel, list)
        newsAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        newsAdapter.isHomePage = false
        binding.recyclerSaved.adapter = newsAdapter
        binding.recyclerSaved.setHasFixedSize(true)
        binding.recyclerSaved.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.overflowMenu = menu
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.bookmark_delete, menu)
        menu.findItem(R.id.delete_bookmark).isVisible = viewModel.articleList.isNotEmpty()

        if (viewModel.articleList.isEmpty())
            overflowMenu.findItem(R.id.delete_bookmark).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.delete_bookmark) {
            if (viewModel.selectedNewsListInBookmarks.isNotEmpty()) {
                // When deleting the list, it will affect the reference
                // So, we store the copy instead of saving the same reference.
                val temp = viewModel.selectedNewsListInBookmarks.map { it }
                lifecycleScope.launch(Dispatchers.Main) {
                    val task = async(Dispatchers.IO) {
                        for (article in viewModel.selectedNewsListInBookmarks) {
                            viewModel.deleteArticle(article)
                            article.isChecked = false
                        }
                    }
                    task.await()

                    newsAdapter.notifyDataSetChanged()
                    newsAdapter.isCheckboxEnabled = false
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Selected Article(s) removed from bookmarks successfully",
                        Snackbar.LENGTH_SHORT
                    )
                        .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                            override fun onShown(transientBottomBar: Snackbar?) {
                                super.onShown(transientBottomBar)
                            }

                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                super.onDismissed(transientBottomBar, event)
                                viewModel.unSelectAllArticles()
                            }
                        })
                    snackbar.setAction("Undo") {
                        for (article in temp) {
                            lifecycleScope.launch {
                                article.id = viewModel.insertArticle(article).toInt()
                            }
                        }
                        snackbar.dismiss()
                    }
                    snackbar.show()
                    snackbar.view.setOnClickListener { snackbar.dismiss() }
                }

//                for(article in viewModel.selectedNewsListInBookmarks) {
//                    viewModel.deleteArticle(article)
//                    article.isChecked = false
//                }
//                overflowMenu.findItem(R.id.unselect1).isVisible = false
//                val snackbar = Snackbar.make(requireView(), "Selected Article(s) removed from bookmarks successfully", Snackbar.LENGTH_SHORT)
//                viewModel.selectedNewsListInBookmarks.clear()
//                viewModel.clearSelectedItemsInBookmark()
//                viewModel.articleList.clear()
//                snackbar.setAction("Undo") {
//                    for (article in temp) {
//                        lifecycleScope.launch {
//                            viewModel.insertArticle(article)
//                        }
//                    }
//                    overflowMenu.findItem(R.id.unselect1).isVisible = false
//                    snackbar.dismiss()
//                }
//                snackbar.show()
//                snackbar.view.setOnClickListener { snackbar.dismiss() }
            }
            else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
                builder.setMessage("Do you want to remove all the bookmarks?")
                    .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            // When deleting the list, it will affect the reference
                            // So, we store the copy instead of saving the same reference.
                            val temp = viewModel.articleList.map { it }
                            for(article in viewModel.articleList) {
                                viewModel.deleteArticle(article)
                                article.isChecked = false
                            }
                            val snackbar = Snackbar.make(requireView(), "All Article(s) removed from bookmarks successfully", Snackbar.LENGTH_SHORT)
                            viewModel.articleList.clear()
                            snackbar.setAction("Undo") {
                                for (article in temp) {
                                    lifecycleScope.launch {
                                        article.id = viewModel.insertArticle(article).toInt()
                                    }
                                }
                                snackbar.dismiss()
                            }
                            snackbar.show()
                            snackbar.view.setOnClickListener { snackbar.dismiss() }
                        }
                    })
                    .setNegativeButton("No", null)

                val alert: AlertDialog = builder.create()
                alert.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /*var isLoading: Boolean = false
    var isLastPage: Boolean = false
    var isScrolling: Boolean = false

    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScrolling = true
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isNotLoadingAndNotLastPage = !isLoading && !isLastPage
            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isNotAtBeginning = firstVisibleItemPosition >= 0
            val isTotalMoreThanVisible = totalItemCount >= Constants.QUERY_PAGE_SIZE
            val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning &&
                    isTotalMoreThanVisible && isScrolling
            if(shouldPaginate) {
                viewModel.getBreakingNews()
                isScrolling = false
            }
        }
    }*/

    fun isCheckboxEnable(): Boolean {
        return newsAdapter.isCheckboxEnabled
    }

    fun clearAdapterCheckboxes() {
        newsAdapter.isCheckboxEnabled = false
        viewModel.unSelectBookmarkArticles()
        newsAdapter.notifyDataSetChanged()
    }

}