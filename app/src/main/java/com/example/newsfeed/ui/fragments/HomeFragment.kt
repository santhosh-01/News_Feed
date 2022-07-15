package com.example.newsfeed.ui.fragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
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
import com.example.newsfeed.util.Resource
import com.example.newsfeed.util.listener.InfiniteScrollListener
import com.example.newsfeed.util.listener.OnArticleClickListener
import com.example.newsfeed.util.listener.OnManageItemsInViewModel
import com.example.newsfeed.viewmodel.NewsViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.min


class HomeFragment : Fragment(), InfiniteScrollListener.OnLoadMoreListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: NewsViewModel
    private var newsAdapter: NewsAdapter? = null
    private var searchNewsAdapter: NewsAdapter? = null
    private lateinit var sharedPref: SharedPreferences
    private lateinit var infiniteScrollListener: InfiniteScrollListener
    private var sortBy: String = "relevancy"
    private var map: HashMap<String, String> = hashMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater)

        sharedPref = requireActivity().getSharedPreferences("application", Context.MODE_PRIVATE)

        requireActivity().title = ""

        setHasOptionsMenu(true)

//        val application = requireActivity().application

//        val newsRepository = NewsRepository(ArticleDatabase.getInstance(requireContext()))
//        val newsViewModelFactory =  NewsViewModelFactory(application, newsRepository)
//        viewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)

        viewModel = (activity as MainActivity).viewModel

        if (!viewModel.isSearchQueryStackEmpty())
            requireActivity().findViewById<SearchView>(R.id.search_view)
                .setQuery(viewModel.getPeekElementFromSearchQueryStack(), false)

        val currentCategory = sharedPref.getString("category", "")
        val currentCountry = sharedPref.getString("country", "")

        if (viewModel.selectedCategory != currentCategory || viewModel.selectedCountry != currentCountry) {
            viewModel.clearSearchQueryStack()
            requireActivity().findViewById<SearchView>(R.id.search_view).setQuery("", false)
        }

        if (viewModel.isSearchQueryStackEmpty())
            setUpRecyclerView(mutableListOf())
        else
            setUpSearchNewsRecyclerView(mutableListOf())

        if (viewModel.selectedCategory != currentCategory || viewModel.selectedCountry != currentCountry) {
            viewModel.breakingNewsPage = 0
            viewModel.breakingNewsResponse = null
            newsAdapter?.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT
            viewModel.initBreakingNews()
            viewModel.selectedCategory = currentCategory!!
            viewModel.selectedCountry = currentCountry!!
        }

        requireActivity().findViewById<ImageButton>(R.id.navigationIcon).setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToCategoryFragment()
            requireView().findNavController().navigate(action)
        }

        viewModel.breakingNews.observe(viewLifecycleOwner, Observer { response ->
            if (newsAdapter == null) {
                setUpRecyclerView(mutableListOf())
            }
            when (response) {
                is Resource.Loading -> {
                    if (viewModel.breakingNewsPage == 1)
                        binding.progressBarMiddle.visibility = View.VISIBLE
                    else
                        newsAdapter?.addNullData()
                }
                is Resource.Success -> {
                    response.data?.let {
                        // Checking whether search is On when going to other pages and returning back to the same page
//                        if (viewModel.searchQueryStack.size == 1) {
//                        }
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
                                newsAdapter!!.removeNull()
                            newsAdapter!!.addData(articleList)
                            infiniteScrollListener.setLoaded()

                            val totalRecords = min(TOTAL_RECORD, response.data.totalResults)
                            val totalPages = ceil(totalRecords.toFloat() / QUERY_PAGE_SIZE).toInt()
                            val isLastPage = viewModel.breakingNewsPage == totalPages
                            if (isLastPage) {
                                infiniteScrollListener.pauseScrollListener(true)
                                binding.recyclerMain.setPadding(0, 0, 0, 0)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    response.message?.let { message ->
                        Toast.makeText(
                            requireContext(),
                            "Error Occurred!! $message",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })

        initSearchFunctionality()

        return binding.root
    }

    private fun showDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.bottom_sheet_layout)

        val relevance = dialog.findViewById<RadioButton>(R.id.relevance)
        val popularity = dialog.findViewById<RadioButton>(R.id.popularity)
        val time = dialog.findViewById<RadioButton>(R.id.time)

        when (sortBy) {
            "relevancy" -> relevance.isChecked = true
            "popularity" -> popularity.isChecked = true
            "publishedAt" -> time.isChecked = true
        }

        val languages = resources.getStringArray(com.example.newsfeed.R.array.language_code_array)

        for (languageNameWithLanguageAbbr in languages) {
            val (languageName, languageAbbr) = languageNameWithLanguageAbbr.split(" - ")
            map[languageAbbr.lowercase()] = languageName
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, languages)

        val autoCompleteTextView =
            dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView)

        autoCompleteTextView.setAdapter(arrayAdapter)

        dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView)
            .addTextChangedListener {
                dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView)
                    .setTextColor(Color.BLACK)
            }

//        autoCompleteTextView.postDelayed({ autoCompleteTextView.showDropDown() }, 500)
        autoCompleteTextView.hint = ("${map[viewModel.preferredLanguage]} - ${viewModel.preferredLanguage.uppercase()}")
        autoCompleteTextView.setHintTextColor(Color.BLACK)

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.window?.setGravity(Gravity.BOTTOM)

        dialog.findViewById<Button>(R.id.get_news_button).setOnClickListener {
            val radiogroup = dialog.findViewById<RadioGroup>(R.id.sort_radio_group)
            val selectedId: Int = radiogroup.checkedRadioButtonId
            val selectedRadioButton = dialog.findViewById<RadioButton>(selectedId)
            when (selectedRadioButton.text) {
                "Relevance" -> {
                    relevance.isChecked = true
                    dialog.dismiss()
                    saveSortingParameterForSearchQuery("relevancy")
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        savePreferredLanguage(
                        dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.split(
                            " - "
                        )[1].lowercase()
                    )
                    requireActivity().findViewById<SearchView>(R.id.search_view)
                        .setQuery(viewModel.popFromSearchQueryStack(), true)
                    saveSortingParameterForSearchQuery("relevancy")
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        savePreferredLanguage(
                        dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.split(
                            " - "
                        )[1].lowercase()
                    )
                }
                "Popularity" -> {
                    popularity.isChecked = true
                    dialog.dismiss()
                    saveSortingParameterForSearchQuery("popularity")
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        savePreferredLanguage(
                        dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.split(
                            " - "
                        )[1]
                    )
                    requireActivity().findViewById<SearchView>(R.id.search_view)
                        .setQuery(viewModel.popFromSearchQueryStack(), true)
                    saveSortingParameterForSearchQuery("popularity")
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        savePreferredLanguage(
                        dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.split(
                            " - "
                        )[1]
                    )
                }
                "Time" -> {
                    time.isChecked = true
                    dialog.dismiss()
                    saveSortingParameterForSearchQuery("publishedAt")
//                    Toast.makeText(requireContext(), autoCompleteTextView.text.toString(), Toast.LENGTH_SHORT).show()
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        savePreferredLanguage(
                        dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.toString().split(
                            " - "
                        )[1]
                    )
                    requireActivity().findViewById<SearchView>(R.id.search_view)
                        .setQuery(viewModel.popFromSearchQueryStack(), true)
                    saveSortingParameterForSearchQuery("publishedAt")
                    if (autoCompleteTextView.text.toString().isNotEmpty())
                        savePreferredLanguage(
                        dialog.findViewById<AutoCompleteTextView>(R.id.languageAutoCompleteTextView).text.toString().split(
                            " - "
                        )[1]
                    )
                }
            }
        }
    }

    private fun savePreferredLanguage(language: String) {
        viewModel.preferredLanguage = language.lowercase()
    }

    private fun saveSortingParameterForSearchQuery(sortBy: String) {
        this.sortBy = sortBy
    }

    private fun initSearchFunctionality() {
        val searchView = requireActivity().findViewById<SearchView>(R.id.search_view)

        var job: Job? = null

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                setUpSearchNewsRecyclerView(mutableListOf())
                query?.let {
                    viewModel.pushToSearchQueryStack(query)
                    viewModel.searchNewsPage = 0
                    viewModel.searchNewsResponse = null
                    viewModel.searchNews(query, sortBy)
                    sortBy = "relevancy"
                    viewModel.preferredLanguage = "en"
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

        viewModel.searchNews.observe(viewLifecycleOwner, Observer { response ->
            if (searchNewsAdapter == null) {
                setUpSearchNewsRecyclerView(mutableListOf())
            }
            when (response) {
                is Resource.Loading -> {
//                    Log.i("HomeFragment", viewModel.searchNewsPage.toString())
                    if (viewModel.searchNewsPage == 1)
                        binding.progressBarMiddle.visibility = View.VISIBLE
                    else
                        searchNewsAdapter?.addNullData()
                }
                is Resource.Success -> {
                    if (response.data == null) {
                        Toast.makeText(requireContext(), "No Result Found", Toast.LENGTH_SHORT)
                    }
                    response.data?.let {
                        if (response.data.totalResults == 0) {
                            Toast.makeText(requireContext(), "No Result Found", Toast.LENGTH_SHORT)
                        }
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
                                searchNewsAdapter!!.removeNull()
                            searchNewsAdapter!!.addData(response.data.articles)
                            infiniteScrollListener.setLoaded()

                            viewModel.unSelectAllArticles()

                            val totalRecords = min(TOTAL_RECORD, response.data.totalResults)
                            val totalPages = ceil(totalRecords.toFloat() / QUERY_PAGE_SIZE).toInt()
                            val isLastPage = viewModel.searchNewsPage == totalPages
                            if (isLastPage) {
                                infiniteScrollListener.pauseScrollListener(true)
                                binding.recyclerMain.setPadding(0, 0, 0, 0)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    response.message?.let { message ->
                        Toast.makeText(
                            requireContext(),
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    binding.progressBarMiddle.visibility = View.GONE
                }
            }
        })
    }

    private fun bookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            article.id = viewModel.insertArticle(article).toInt()
        }
    }

    private fun unbookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.deleteArticle(viewModel.getArticleByTitle(article.title))
        }
    }

    private val articleClickListener = object : OnArticleClickListener {
        override fun onClick(article: Article) {
            val action =
                HomeFragmentDirections.actionHomeFragmentToArticlePreviewFragment(article, true)
            requireView().findNavController().navigate(action)

        }

        override fun onBookmarkButtonClick(article: Article) {
            lifecycleScope.launch(Dispatchers.IO) {
                if (!viewModel.isRecordExist(article.title)) {
                    article.isExistInDB = true
                    bookmarkArticle(article)
                    val snackbar = Snackbar.make(
                        requireView(),
                        "Article added to bookmark Successfully!!",
                        Snackbar.LENGTH_SHORT
                    )
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
                newsAdapter!!.canSelectBookmark = false
            } else {
                newsAdapter!!.canSelectBookmark = true
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

    fun setUpRecyclerView(list: MutableList<Article?>) {
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
        newsAdapter?.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.recyclerMain.adapter = newsAdapter
        binding.recyclerMain.setHasFixedSize(true)
    }

    private fun setUpSearchNewsRecyclerView(list: MutableList<Article?>) {
        val linearLayoutManager = LinearLayoutManager(requireContext())
        infiniteScrollListener = InfiniteScrollListener(linearLayoutManager, this)

        binding.recyclerMain.layoutManager = linearLayoutManager
        binding.recyclerMain.addOnScrollListener(infiniteScrollListener)
        searchNewsAdapter = NewsAdapter(articleClickListener, onManageItemsInViewModel, list)
        searchNewsAdapter?.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.recyclerMain.adapter = searchNewsAdapter
        binding.recyclerMain.setHasFixedSize(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.overflow_menu, menu)

        val sort = menu.findItem(R.id.sort)
        val bookmark = menu.findItem(R.id.add_bookmark)

        viewModel.searchQueryStack.observe(viewLifecycleOwner, Observer {
            sort.isVisible = it.isNotEmpty()
        })

        viewModel.selectedItemPositionsInHome.observe(viewLifecycleOwner, Observer {
            bookmark.isVisible = it.isNotEmpty()
        })

        sort.setOnMenuItemClickListener {
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
                    newsAdapter!!.isCheckboxEnabled = false
                    newsAdapter!!.notifyDataSetChanged()
                    if (!viewModel.isSearchQueryStackEmpty()) {
                        searchNewsAdapter!!.isCheckboxEnabled = false
                        searchNewsAdapter!!.notifyDataSetChanged()
                    }
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
                        newsAdapter!!.notifyDataSetChanged()
                        if (!viewModel.isSearchQueryStackEmpty()) {
                            searchNewsAdapter!!.notifyDataSetChanged()
                        }
                    }
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

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager =
            getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun clearAdapterCheckboxes() {
        if (!viewModel.isSearchQueryStackEmpty()) {
            searchNewsAdapter!!.isCheckboxEnabled = false
            viewModel.unSelectAllArticles()
            searchNewsAdapter!!.notifyDataSetChanged()
        } else {
            newsAdapter!!.isCheckboxEnabled = false
            viewModel.unSelectAllArticles()
            newsAdapter!!.notifyDataSetChanged()
        }
    }

    fun isCheckboxEnable(): Boolean {
        return if (!viewModel.isSearchQueryStackEmpty()) searchNewsAdapter!!.isCheckboxEnabled
        else newsAdapter!!.isCheckboxEnabled
    }

    fun notifyAdapterAsDataSetChanged() {
        if (!viewModel.isSearchQueryStackEmpty()) searchNewsAdapter!!.notifyDataSetChanged()
        else newsAdapter!!.notifyDataSetChanged()
    }

    override fun onLoadMore() {
        if (!viewModel.isSearchQueryStackEmpty()) {
            viewModel.searchNews(viewModel.newSearchQuery!!, sortBy)
        } else {
            if (sharedPref.getString("country", "") == "global") {
                viewModel.getBreakingNews(viewModel.selectedCategory)
            } else {
                viewModel.getBreakingNews(
                    viewModel.selectedCategory,
                    sharedPref.getString("country", "")!!
                )
            }
        }
    }
}