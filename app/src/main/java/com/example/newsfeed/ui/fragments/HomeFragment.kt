package com.example.newsfeed.ui.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newsfeed.R
import com.example.newsfeed.adapter.NewsAdapter
import com.example.newsfeed.databinding.FragmentHomeBinding
import com.example.newsfeed.entity.Article
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.util.Constants.Companion.QUERY_PAGE_SIZE
import com.example.newsfeed.util.Constants.Companion.TOTAL_RECORD
import com.example.newsfeed.util.HandleKeyboard.Companion.hideKeyboard
import com.example.newsfeed.util.Resource
import com.example.newsfeed.util.listener.InfiniteScrollListener
import com.example.newsfeed.util.listener.OnArticleClickListener
import com.example.newsfeed.util.listener.OnManageItemsInViewModel
import com.example.newsfeed.viewmodel.NewsViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlin.math.ceil
import kotlin.math.min


class HomeFragment : Fragment(), InfiniteScrollListener.OnLoadMoreListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private lateinit var dialog: Dialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater)

        initAllElements()

        return binding.root
    }

    private fun initAllElements() {
        initAttributes()
        initUIElements()
        initLiveDataObserverForBreakingNews()
        initUIElementForSearch()
        initLiveDataObserverForSearch()
        setHasOptionsMenu(true)
    }

    private fun initAttributes() {
        viewModel = (activity as MainActivity).viewModel
    }

    private fun initUIElements() {
        if (!viewModel.isSearchQueryStackEmpty())
            requireActivity().findViewById<SearchView>(R.id.search_view)
                .setQuery(viewModel.getPeekElementFromSearchQueryStack(), false)
        else
            requireActivity().findViewById<SearchView>(R.id.search_view).setQuery("", false)

        if (viewModel.isCategoryOrCountryChanged()) {
            viewModel.clearSearchQueryStack()
            requireActivity().findViewById<SearchView>(R.id.search_view).setQuery("", false)
        }

        // Setting up Recycler View
        setUpRecyclerView(mutableListOf())

        if (viewModel.isCategoryOrCountryChanged()) {
            newsAdapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT
            viewModel.clearAdapterAndGetBreakingNews()
            viewModel.changeCategoryAndCountryOnViewModel()
        }

        requireActivity().findViewById<ImageButton>(R.id.navigationIcon).setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToCategoryFragment()
            requireView().findNavController().navigate(action)
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = true
            binding.recyclerMain.visibility = View.GONE
            if (requireActivity().findViewById<SearchView>(R.id.search_view).query.isEmpty()) {
                newsAdapter.clearAdapterList()
                viewModel.clearSearchQueryStack()
                viewModel.initAccordingToCurrentConfig()
            } else {
                newsAdapter.clearAdapterList()
                viewModel.initSearchAccordingToCurrentConfig()
            }
            clearAdapterCheckboxes()
        }
    }

    private fun initLiveDataObserverForBreakingNews() {
        viewModel.breakingNews.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Loading -> {
                    if (viewModel.breakingNewsPage == 1 && !binding.swipeRefreshLayout.isRefreshing) {
                        clearAdapter()
                        binding.recyclerMain.visibility = View.GONE
                        binding.progressBarMiddle.visibility = View.VISIBLE
                        infiniteScrollListener.pauseScrollListener(false)
                    }
                    else if (!binding.swipeRefreshLayout.isRefreshing)
                        newsAdapter.addNullData()
                }
                is Resource.Success -> {
                    if (viewModel.isSearchQueryStackEmpty()) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        response.data?.let {
                            val articleList = response.data.articles
                            lifecycleScope.launch(Dispatchers.Main) {
                                val task = async(Dispatchers.IO) {
                                    articleList.forEach { article ->
                                        article.isExistInDB = viewModel.isRecordExist(article.title)
                                        if (article.isExistInDB)
                                            article.id = viewModel.getArticleByTitle(article.title).id
                                    }
                                }
                                task.await()
                                if (viewModel.breakingNewsPage == 1) {
                                    binding.progressBarMiddle.visibility = View.INVISIBLE
                                } else
                                    newsAdapter.removeNull()
                                binding.recyclerMain.visibility = View.VISIBLE
                                newsAdapter.addData(articleList)
                                infiniteScrollListener.setLoaded()

                                val totalRecords = min(TOTAL_RECORD, response.data.totalResults)
                                val totalPages = ceil(totalRecords.toFloat() / QUERY_PAGE_SIZE).toInt()
                                val isLastPage = viewModel.breakingNewsPage == totalPages
                                if (isLastPage) {
                                    infiniteScrollListener.pauseScrollListener(true)
                                    binding.recyclerMain.setPadding(0, 0, 0, 0)
                                }
                                viewModel.breakingNewsPage++
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    response.message?.let { message ->
                        if (message == "No internet connection") {
                            Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.progressBarMiddle.visibility = View.GONE
                        } else {
                            var isSuccess: Boolean = false
                            Log.i("HomeFragment", Thread.currentThread().name.toString() + "1")
                            lifecycleScope.launch(Dispatchers.Main) {
                                Log.i("HomeFragment", Thread.currentThread().name.toString() + "2")
                                withContext(Dispatchers.IO) {
                                    Log.i(
                                        "HomeFragment",
                                        Thread.currentThread().name.toString() + "3"
                                    )
                                    isSuccess = viewModel.changeAPIKey()
                                }
                                if (!isSuccess) {
                                    Toast.makeText(
                                        requireContext(),
                                        "ALL API Keys are used!! Try again after some time",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    binding.progressBarMiddle.visibility = View.GONE
                                } else {
                                    /*Toast.makeText(
                                        requireContext(),
                                        "API Request limit completed!!, So, Trying to fetch news from other API Key ${viewModel.apiKey}",
                                        Toast.LENGTH_LONG
                                    ).show()*/
                                    binding.progressBarMiddle.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initUIElementForSearch() {
        val searchView = requireActivity().findViewById<SearchView>(R.id.search_view)

//        var job: Job? = null

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                newsAdapter.clearAdapterList()
                query?.let {
                    viewModel.sortBy = "relevancy"
                    viewModel.preferredLanguage = "en"
                    viewModel.clearListAndGetSearchNews(query)
                }
                hideKeyboard()
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
//                requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_bar).visibility = View.GONE
                /*if (newText.isNullOrBlank()) {

                }
                job?.cancel()
                job = MainScope().launch {
                    delay(Constants.SEARCH_NEWS_TIME_DELAY)
                    if (!newText.isNullOrBlank()) {
                        viewModel.searchNews(newText.toString())
                        viewModel.searchNewsResponse = null
                    }
                }*/
                return true
            }

        })
    }

    private fun explicitSearch(query: String?) {
        newsAdapter.clearAdapterList()
        query?.let {
            viewModel.clearListAndGetSearchNews(query)
        }
        hideKeyboard()
    }

    private fun initLiveDataObserverForSearch() {
        viewModel.searchNews.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Loading -> {
//                    Log.i("HomeFragment", viewModel.searchNewsPage.toString())
                    if (viewModel.searchNewsPage == 1 && !binding.swipeRefreshLayout.isRefreshing) {
                        clearAdapter()
                        binding.recyclerMain.visibility = View.GONE
                        binding.progressBarMiddle.visibility = View.VISIBLE
                        infiniteScrollListener.pauseScrollListener(false)
                    }
                    else if (!binding.swipeRefreshLayout.isRefreshing)
                        newsAdapter.addNullData()
                }
                is Resource.Success -> {
                    if (!viewModel.isSearchQueryStackEmpty()) {
                        binding.swipeRefreshLayout.isRefreshing = false
                        response.data?.let {
                            val articleList = response.data.articles
                            lifecycleScope.launch(Dispatchers.Main) {
                                val task = async(Dispatchers.IO) {
                                    articleList.forEach { article ->
                                        article.isExistInDB = viewModel.isRecordExist(article.title)
                                    }
                                }
                                task.await()
                                if (viewModel.searchNewsPage == 1)
                                    binding.progressBarMiddle.visibility = View.INVISIBLE
                                else
                                    newsAdapter.removeNull()
                                binding.recyclerMain.visibility = View.VISIBLE
                                newsAdapter.addData(response.data.articles)
                                infiniteScrollListener.setLoaded()

                                viewModel.unSelectAllArticles()

                                val totalRecords = min(TOTAL_RECORD, response.data.totalResults)
                                val totalPages = ceil(totalRecords.toFloat() / QUERY_PAGE_SIZE).toInt()
                                val isLastPage = viewModel.searchNewsPage == totalPages
                                if (isLastPage) {
                                    infiniteScrollListener.pauseScrollListener(true)
                                    binding.recyclerMain.setPadding(0, 0, 0, 0)
                                }
                                viewModel.searchNewsPage++
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    response.message?.let { message ->
                        if (message == "No internet connection" || message == "No Result Found!!") {
                            Toast.makeText(
                                requireContext(),
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                            binding.progressBarMiddle.visibility = View.GONE
                        } else {
                            val isSuccess = runBlocking { viewModel.changeAPIKeyForSearch() }
                            if (!isSuccess) {
                                Toast.makeText(
                                    requireContext(),
                                    "ALL API Keys are used!! Try again after some time",
                                    Toast.LENGTH_LONG
                                ).show()
                                binding.progressBarMiddle.visibility = View.GONE
                            } else {
                                /*Toast.makeText(
                                    requireContext(),
                                    "$message, So, Trying to fetch news from other API Key ${viewModel.apiKey}",
                                    Toast.LENGTH_LONG
                                ).show()*/
                                binding.progressBarMiddle.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.overflow_menu, menu)

        val advancedSearch = menu.findItem(R.id.advanced_search)
        val bookmark = menu.findItem(R.id.add_bookmark)

        viewModel.searchQueryStack.observe(viewLifecycleOwner) {
            advancedSearch.isVisible = it.isNotEmpty()
        }

        viewModel.selectedItemPositionsInHome.observe(viewLifecycleOwner) {
            bookmark.isVisible = it.isNotEmpty()
        }

        advancedSearch.setOnMenuItemClickListener {
            showDialog()
            true
        }

//        val menuItem = menu.findItem(R.id.search_view)
//        val searchView = menuItem.actionView as SearchView
//        searchView.queryHint = "Search here..."
//        searchView.onActionViewExpanded()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_bookmark) {
            if (viewModel.selectedNewsListInHome.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val task = async(Dispatchers.IO) {
                        for (article in viewModel.selectedNewsListInHome) {
                            article.isChecked = false
                            article.id = viewModel.insertArticle(article).toInt()
                            article.isExistInDB = true
                        }
                    }
                    task.await()
                    newsAdapter.isCheckboxEnabled = false
                    newsAdapter.notifyDataSetChanged()
                    val temp = viewModel.selectedNewsListInHome
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Selected Article(s) added to bookmarks successfully",
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
                            article.isExistInDB = false
                            viewModel.deleteArticle(article)
                        }
                        snackbar.dismiss()
                        newsAdapter.notifyDataSetChanged()
                    }
                    (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                    snackbar.show()
                    snackbar.view.setOnClickListener { snackbar.dismiss() }
                }
            } else {
                Toast.makeText(
                    requireActivity(),
                    "No News selected!!\nPlease Long press to select the news",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val navController: NavController = requireView().findNavController()
        return NavigationUI.onNavDestinationSelected(item, navController) ||
                super.onOptionsItemSelected(item)
    }


    private fun showDialog() {
        inflateDialog()
        initDialogUIElements()
        onClickListenerToFetchAdvancedSearch()
    }

    private fun inflateDialog() {
        dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_layout)

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.show()
    }

    private fun initDialogUIElements() {
        val relevance = dialog.findViewById<RadioButton>(R.id.relevance)
        val popularity = dialog.findViewById<RadioButton>(R.id.popularity)
        val time = dialog.findViewById<RadioButton>(R.id.time)
        val autoCompleteTextView =
            dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView)

        when (viewModel.sortBy) {
            "relevancy" -> relevance.isChecked = true
            "popularity" -> popularity.isChecked = true
            "publishedAt" -> time.isChecked = true
        }

        val languages = resources.getStringArray(R.array.language_code_array)

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, languages)

        autoCompleteTextView.setAdapter(arrayAdapter)

//        autoCompleteTextView.postDelayed({ autoCompleteTextView.showDropDown() }, 500)
        autoCompleteTextView.hint =
            ("${viewModel.languageMap[viewModel.preferredLanguage]} - ${viewModel.preferredLanguage.uppercase()}")
        autoCompleteTextView.setHintTextColor(Color.BLACK)
    }

    private fun onClickListenerToFetchAdvancedSearch() {
        val relevance = dialog.findViewById<RadioButton>(R.id.relevance)
        val popularity = dialog.findViewById<RadioButton>(R.id.popularity)
        val time = dialog.findViewById<RadioButton>(R.id.time)
        val autoCompleteTextView =
            dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView)

        dialog.findViewById<Button>(R.id.get_news_button).setOnClickListener {
            val radiogroup = dialog.findViewById<RadioGroup>(R.id.sort_radio_group)
            val selectedId: Int = radiogroup.checkedRadioButtonId
            val selectedRadioButton = dialog.findViewById<RadioButton>(selectedId)
            when (selectedRadioButton.text) {
                "Relevance" -> {
                    relevance.isChecked = true
                    viewModel.sortBy = "relevancy"
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        viewModel.preferredLanguage =
                            dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.split(
                                " - "
                            )[1].lowercase()
                    explicitSearch(viewModel.popFromSearchQueryStack())
                    dialog.dismiss()
                }
                "Popularity" -> {
                    popularity.isChecked = true
                    viewModel.sortBy = "popularity"
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        viewModel.preferredLanguage =
                            dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.split(
                                " - "
                            )[1].lowercase()
                    explicitSearch(viewModel.popFromSearchQueryStack())
                    dialog.dismiss()
                }
                "Time" -> {
                    time.isChecked = true
                    viewModel.sortBy = "publishedAt"
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        viewModel.preferredLanguage =
                            dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.toString()
                                .split(
                                    " - "
                                )[1].lowercase()
                    explicitSearch(viewModel.popFromSearchQueryStack())
                    dialog.dismiss()
                }
            }
        }
    }


    private fun bookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            article.id = viewModel.insertArticle(article).toInt()
            article.isExistInDB = true
        }
    }

    private fun unbookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.getArticleByTitle(article.title).isExistInDB = false
            viewModel.deleteArticle(viewModel.getArticleByTitle(article.title))
        }
    }

    private val articleClickListener = object : OnArticleClickListener {
        override fun onClick(article: Article) {
            val action =
                HomeFragmentDirections.actionHomeFragmentToArticlePreviewFragment(
                    article.title,
                    true
                )
            requireView().findNavController().navigate(action)
        }

        override fun onBookmarkButtonClick(article: Article) {
            lifecycleScope.launch(Dispatchers.IO) {
                if (!article.isExistInDB) {
                    article.isExistInDB = true
                    bookmarkArticle(article)
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Article added to bookmark Successfully!!",
                        Snackbar.LENGTH_SHORT
                    )
                    (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                    snackbar.show()
                    snackbar.view.setOnClickListener { snackbar.dismiss() }
                } else {
                    article.isExistInDB = false
                    unbookmarkArticle(article)
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Article removed from bookmark Successfully!!",
                        Snackbar.LENGTH_SHORT
                    )
                    (requireActivity() as MainActivity).customizeSnackBar(snackbar)
                    snackbar.show()
                    snackbar.view.setOnClickListener { snackbar.dismiss() }
                }
            }
//            newsAdapter.notifyDataSetChanged()
        }

        override fun onShareButtonClick(article: Article) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, article.url)
            startActivity(shareIntent)
        }

        override fun onLongClick(article: Article, cardView: MaterialCardView) {
            if (article.isExistInDB) {
                Toast.makeText(
                    requireActivity(), "The article was already bookmarked!!\n" +
                            "So you can't select this article", Toast.LENGTH_SHORT
                ).show()
                newsAdapter.canSelectBookmark = false
            } else {
                newsAdapter.canSelectBookmark = true
            }
        }

    }

    private val onManageItemsInViewModel: OnManageItemsInViewModel =
        object : OnManageItemsInViewModel {
            override fun addSelectedItemToList(article: Article) {
                viewModel.selectedNewsListInHome.add(article)
            }

            override fun removeUnselectedItemFromList(article: Article) {
                viewModel.selectedNewsListInHome.remove(article)
            }

            override fun addSelectedItemPositionToList(position: Int) {
                viewModel.addSelectedItemPositionInHome(position)
            }

            override fun removeUnselectedPositionFromList(position: Int) {
                viewModel.removeUnselectedItemPositionFromHome(position)
            }

        }

    private fun setUpRecyclerView(list: MutableList<Article?>) {
        val linearLayoutManager = LinearLayoutManager(requireContext())
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)

        binding.recyclerMain.layoutManager = linearLayoutManager
        binding.recyclerMain.addOnScrollListener(infiniteScrollListener)
        /* binding.recyclerMain.addItemDecoration(
             DividerItemDecoration(
                 requireContext(),
                 DividerItemDecoration.VERTICAL
             )
         )*/
        newsAdapter = NewsAdapter(articleClickListener, onManageItemsInViewModel, list)
        newsAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.recyclerMain.adapter = newsAdapter
        binding.recyclerMain.setHasFixedSize(true)
    }


    fun clearAdapterCheckboxes() {
        newsAdapter.isCheckboxEnabled = false
        viewModel.unSelectAllArticles()
        newsAdapter.notifyDataSetChanged()
    }

    fun isCheckboxEnable(): Boolean {
        return newsAdapter.isCheckboxEnabled
    }


    override fun onLoadMore() {
        if (!viewModel.isSearchQueryStackEmpty()) {
            viewModel.searchNews(viewModel.searchQuery!!, viewModel.sortBy)
        } else {
            viewModel.getBreakingNews(
                viewModel.selectedCategory,
                viewModel.selectedCountry
            )
        }
    }

    fun clearAdapter() {
        newsAdapter.clearAdapterList()
    }

}