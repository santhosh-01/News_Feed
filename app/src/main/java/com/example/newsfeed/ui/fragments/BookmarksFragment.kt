package com.example.newsfeed.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsfeed.R
import com.example.newsfeed.adapter.NewsAdapter
import com.example.newsfeed.databinding.FragmentBookmarksBinding
import com.example.newsfeed.entity.Article
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.util.HandleKeyboard.Companion.hideKeyboard
import com.example.newsfeed.util.listener.OnArticleClickListener
import com.example.newsfeed.util.listener.OnManageItemsInViewModel
import com.example.newsfeed.viewmodel.NewsViewModel
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
    ): View {

        binding = FragmentBookmarksBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        initUIElements()

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initUIElements() {
        // newsAdapter = NewsAdapter(articleClickListener,onAddItemToList, listOf())
        setUpRecyclerView(mutableListOf())
        requireActivity().findViewById<SearchView>(R.id.search_view)
            .setQuery("", false)

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            clearAdapterCheckboxes()
            setUpRecyclerView(mutableListOf())
            if (viewModel.bookmarkSearchQuery != null) {
                viewModel.bookmarkSearchQuery = null
                requireActivity().findViewById<SearchView>(R.id.search_view).setQuery("",false)
                newsAdapter.loadList(viewModel.temp.reversed())
                viewModel.temp.clear()
            }
            else {
                newsAdapter.loadList(viewModel.articleList.reversed())
            }
            binding.swipeRefreshLayout.isRefreshing = false
            newsAdapter.notifyDataSetChanged()
//            Log.i("BookmarkFragment", viewModel.temp.size.toString())
//            Log.i("BookmarkFragment", viewModel.temp.size.toString())
        }

        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchView = requireActivity().findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    val filteredList = viewModel.filterBookmarkNews(query.toString())
                    if (filteredList.isEmpty()) {
                        Toast.makeText(requireContext(), "No Matching Record Found!!", Toast.LENGTH_LONG).show()
                        newsAdapter.clearAdapterList()
                        newsAdapter.notifyDataSetChanged()
                    }
                    else
                        newsAdapter.filterList(filteredList)
                    hideKeyboard()
                    searchView.clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return false
                }
            })

        viewModel.getSavedNewsArticles().observe(viewLifecycleOwner) { articles ->
            viewModel.articleList.clear()
            viewModel.articleList.addAll(articles)
            if (articles.isNotEmpty()) {
                binding.recyclerSaved.visibility = View.VISIBLE
                overflowMenu.findItem(R.id.delete_bookmark).isVisible = true
                articles.forEach { article ->
                    article.isExistInDB = true
                }
                newsAdapter.loadList(articles.reversed())
                binding.noBookmarkImage.visibility = View.GONE
            } else {
                binding.recyclerSaved.visibility = View.GONE
                overflowMenu.findItem(R.id.delete_bookmark).isVisible = false
                binding.noBookmarkImage.visibility = View.VISIBLE
            }
        }
    }

    private fun unbookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getArticleByTitle(article.title!!).isExistInDB = false
            viewModel.deleteArticle(viewModel.getArticleByTitle(article.title))
        }
        article.isExistInDB = false
    }

    private val articleClickListener = object : OnArticleClickListener {
        override fun onClick(article: Article) {
            val action =
                BookmarksFragmentDirections.actionBookmarksFragmentToArticlePreviewFragment(
                    article.title!!,
                    false
                )
            requireView().findNavController().navigate(action)
        }

        override fun onBookmarkButtonClick(article: Article) {
            unbookmarkArticle(article)
            (requireActivity() as MainActivity).slideUpBottomNavBar()
            val snackbar = Snackbar.make(
                requireView(),
                "Article removed from bookmarks successfully",
                Snackbar.LENGTH_SHORT
            )
            snackbar.setAction("Undo") {
                lifecycleScope.launch {
                    viewModel.clearSelectedItemsInBookmark()
                    article.id = viewModel.insertArticle(article).toInt()
                    article.isExistInDB = true
                }
                (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                snackbar.dismiss()
            }
            (requireActivity() as MainActivity).customizeSnackBar(snackbar)
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
        newsAdapter = NewsAdapter(articleClickListener, onManageItemsInViewModel, list)
        newsAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
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
            (requireActivity() as MainActivity).slideUpBottomNavBar()
            if (viewModel.selectedNewsListInBookmarks.isNotEmpty()) {
                // When deleting the list, it will affect the reference
                // So, we store the copy instead of saving the same reference.
                val temp = viewModel.selectedNewsListInBookmarks.map { it }
                lifecycleScope.launch(Dispatchers.Main) {
                    val task = async(Dispatchers.IO) {
                        for (article in viewModel.selectedNewsListInBookmarks) {
                            article.isExistInDB = false
                            viewModel.deleteArticle(article)
                            article.isChecked = false
                        }
                    }
                    task.await()

                    viewModel.unSelectBookmarkArticles()
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
                            }
                        })
                    snackbar.setAction("Undo") {
                        for (article in temp) {
                            lifecycleScope.launch {
                                viewModel.clearSelectedItemsInBookmark()
                                article.id = viewModel.insertArticle(article).toInt()
                                article.isExistInDB = true
                            }
                        }
                        (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                        snackbar.dismiss()
                    }
                    (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                    snackbar.show()
                    snackbar.view.setOnClickListener { snackbar.dismiss() }
                }
            } else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
                builder.setMessage("Do you want to remove all the bookmarks?")
                    .setPositiveButton("Yes"
                    ) { dialog, which -> // When deleting the list, it will affect the reference
                        // So, we store the copy instead of saving the same reference.
                        val temp = viewModel.articleList.map { it }
                        for (article in viewModel.articleList) {
                            viewModel.deleteArticle(article)
                            article.isChecked = false
                        }
                        val snackbar = Snackbar.make(
                            requireView(),
                            "All Article(s) removed from bookmarks successfully",
                            Snackbar.LENGTH_SHORT
                        )
                        viewModel.articleList.clear()
                        snackbar.setAction("Undo") {
                            for (article in temp) {
                                lifecycleScope.launch {
                                    article.id = viewModel.insertArticle(article).toInt()
                                    article.isExistInDB = true
                                }
                            }
                            (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                            snackbar.dismiss()
                        }
                        (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                        snackbar.show()
                        snackbar.view.setOnClickListener { snackbar.dismiss() }
                    }
                    .setNegativeButton("No", null)

                val alert: AlertDialog = builder.create()
                alert.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun isCheckboxEnable(): Boolean {
        return newsAdapter.isCheckboxEnabled
    }

    fun clearAdapterCheckboxes() {
        newsAdapter.isCheckboxEnabled = false
        viewModel.unSelectBookmarkArticles()
        newsAdapter.notifyDataSetChanged()
    }

    fun showAllBookmarks() {
        requireActivity().findViewById<SearchView>(R.id.search_view).setQuery("", false)
        viewModel.revertFilterBookmark()
        newsAdapter.loadList(viewModel.articleList.reversed())
    }

}