package com.example.newsfeed.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.widget.SearchView
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
import com.example.newsfeed.ui.fragments.OnManageItemsInViewModel
import com.example.newsfeed.util.Constants
import com.example.newsfeed.util.Constants.Companion.QUERY_PAGE_SIZE
import com.example.newsfeed.util.Constants.Companion.TOTAL_RECORD
import com.example.newsfeed.util.Resource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var viewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(layoutInflater)

//        (activity as AppCompatActivity).supportActionBar?.setIcon(R.drawable.ic_menu_left_lined)

        requireActivity().title = ""

        setHasOptionsMenu(true)

        setUpRecyclerView(mutableListOf())

//        val application = requireActivity().application

//        val newsRepository = NewsRepository(ArticleDatabase.getInstance(requireContext()))
//        val newsViewModelFactory =  NewsViewModelFactory(application, newsRepository)
//        viewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)

        viewModel = (activity as MainActivity).viewModel

        val temp = viewModel.selectedCategory

        val categoryId = HomeFragmentArgs.fromBundle(requireArguments()).category

        viewModel.selectedCategory = when(categoryId) {
            R.id.entertainment -> "entertainment"
            R.id.general -> "general"
            R.id.business -> "business"
            R.id.health -> "health"
            R.id.science -> "science"
            R.id.sports -> "sports"
            R.id.technology -> "technology"
            else -> "general"
        }

        if (viewModel.selectedCategory != temp) {
            viewModel.breakingNewsResponse = null
            newsAdapter.loadList(listOf())
            newsAdapter.notifyDataSetChanged()
            viewModel.initBreakingNews()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<ImageButton>(R.id.navigationIcon).setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToCategoryFragment()
            requireView().findNavController().navigate(action)
        }

        /*var job: Job? = null

        binding.searchQuery.addTextChangedListener { editable ->
            job?.cancel()
            job = MainScope().launch {
                delay(Constants.SEARCH_NEWS_TIME_DELAY)
                if (!editable.isNullOrBlank()) {
                    viewModel.searchNews(editable.toString())
                }
            }
        }*/

        viewModel.breakingNews.observe(viewLifecycleOwner, Observer { response ->
            when(response) {
                is Resource.Loading -> {
                    showProgressBar()
                }
                is Resource.Success -> {
                    hideProgressBar()
                    response.data?.let {
                        val articleList = response.data.articles
                        articleList.forEach { article ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                article.isExistInDB = viewModel.isRecordExist(article.title)
                            }
                        }
                        newsAdapter.loadList(articleList)
                        viewModel.selectedNewsListInHome.clear()
                        viewModel.clearSelectedItemPositionListInHome()

                        val totalPages = TOTAL_RECORD / QUERY_PAGE_SIZE
                        isLastPage = viewModel.breakingNewsPage == totalPages
                    }
                }
                is Resource.Error -> {
                    hideProgressBar()
                    Toast.makeText(requireContext(), "Error Occurred!!", Toast.LENGTH_SHORT).show()
                }
            }
        })

        viewModel.searchNews.observe(viewLifecycleOwner, Observer { response ->
            when(response) {
                is Resource.Loading -> {
                    showProgressBarDuringSearch()
                }
                is Resource.Success -> {
                    hideProgressBarDuringSearch()
                    response.data?.let {
                        newsAdapter.loadList(response.data.articles)
                    }
                }
                is Resource.Error -> {
                    hideProgressBarDuringSearch()
                    Toast.makeText(requireContext(), "Error Occurred!!", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun hideProgressBar() {
        requireActivity().findViewById<ProgressBar>(R.id.progressBar_main).visibility = View.GONE
        isLoading = false
    }

    private fun showProgressBar() {
        requireActivity().findViewById<ProgressBar>(R.id.progressBar_main).visibility = View.VISIBLE
        isLoading = true
    }

    private fun hideProgressBarDuringSearch() {
        requireActivity().findViewById<ProgressBar>(R.id.progressBar_search).visibility = View.GONE
        isLoading = false
    }

    private fun showProgressBarDuringSearch() {
        requireActivity().findViewById<ProgressBar>(R.id.progressBar_search).visibility = View.VISIBLE
        isLoading = true
    }

    private fun bookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.insertArticle(article)
        }
    }

    private fun unbookmarkArticle(article: Article) {
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.deleteArticle(viewModel.getArticleByTitle(article.title))
        }
    }

    private val articleClickListener = object : OnArticleClickListener {
        override fun onClick(article: Article) {
//            startActivity(
//                Intent(requireContext(), DetailsActivity::class.java)
//                .putExtra("data", article))
            val action = HomeFragmentDirections.actionHomeFragmentToArticlePreviewFragment(article, true)
            requireView().findNavController().navigate(action)

//            startActivity(Intent(activity, DetailedNewsActivity::class.java))
        }

        override fun onBookmarkButtonClick(article: Article) {
            lifecycleScope.launch(Dispatchers.IO) {
                if (!viewModel.isRecordExist(article.title)) {
                    article.isExistInDB = true
                    bookmarkArticle(article)
                    val snackbar = Snackbar.make(requireView(), "Article added to bookmark Successfully!!", Snackbar.LENGTH_SHORT)
                    snackbar.show()
                    snackbar.view.setOnClickListener { snackbar.dismiss() }
                } else {
                    article.isExistInDB = false
                    unbookmarkArticle(article)
                    val snackbar = Snackbar.make(requireView(), "Article removed from bookmark Successfully!!", Snackbar.LENGTH_SHORT)
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
                Toast.makeText(requireActivity(),"The article was already bookmarked!!\n" +
                        "So you can't select this article", Toast.LENGTH_SHORT).show()
                cardView.isCheckable = false
            }
            else {
                cardView.isCheckable = true
            }
        }

    }

//    private val onFetchArticleListener = object : OnFetchArticleListener<NewsArticle> {
//        override fun onFetchData(list: List<Article>) {
//            setUpRecyclerView(list)
//        }
//
//        override fun onError(message: String) {
//            Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
//        }
//    }

    private val onManageItemsInViewModel: OnManageItemsInViewModel = object : OnManageItemsInViewModel {
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

    var isLoading: Boolean = false
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
            val isTotalMoreThanVisible = totalItemCount >= QUERY_PAGE_SIZE
            val shouldPaginate = isNotLoadingAndNotLastPage && isAtLastItem && isNotAtBeginning &&
                    isTotalMoreThanVisible && isScrolling
            if(shouldPaginate) {
                viewModel.getBreakingNews(viewModel.selectedCategory)
                isScrolling = false
            }
        }
    }

    private fun setUpRecyclerView(list: MutableList<Article>) {
        newsAdapter = NewsAdapter(articleClickListener, onManageItemsInViewModel, list)
        binding.recyclerMain.adapter = newsAdapter
        binding.recyclerMain.setHasFixedSize(true)
        binding.recyclerMain.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMain.addOnScrollListener(this@HomeFragment.scrollListener)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.overflow_menu, menu)
        val menuItem = menu.findItem(R.id.search_view)
        val searchView = menuItem.actionView as SearchView
        searchView.queryHint = "Search here..."

        var job: Job? = null

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchNews(query.toString())
                hideKeyboard()
//                requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_bar).visibility = View.VISIBLE
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
//                requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_bar).visibility = View.GONE
//                if (newText.isNullOrBlank()) {
//
//                }
                job?.cancel()
                job = MainScope().launch {
                    delay(Constants.SEARCH_NEWS_TIME_DELAY)
                    if (!newText.isNullOrBlank()) {
                        viewModel.searchNews(newText.toString())
                        viewModel.searchNewsResponse = null
                    }
                }
                return true
            }

        })

        menu.findItem(R.id.unselect).isVisible = false
        menu.findItem(R.id.unselect).setOnMenuItemClickListener {
            viewModel.unSelectAllArticles()
            newsAdapter.notifyDataSetChanged()
            it.isVisible = false
            true
        }

        viewModel.selectedItemPositionsInHome.observe(viewLifecycleOwner) { selectedItemPositions ->
            menu.findItem(R.id.unselect).isVisible = selectedItemPositions.isNotEmpty()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_bookmark) {
            if (viewModel.selectedNewsListInHome.isNotEmpty()) {
                var job: Job? = null
                for(article in viewModel.selectedNewsListInHome) {
                    job = lifecycleScope.launch {
                        article.isChecked = false
                        article.id = viewModel.insertArticle(article).toInt()
                    }
                }
                job?.invokeOnCompletion {
                    val temp = viewModel.selectedNewsListInHome.map { it }
                    val snackbar = Snackbar.make(requireView(), "Selected Article(s) added to bookmarks successfully", Snackbar.LENGTH_SHORT)
                    snackbar.setAction("Undo") {
                        for (article in temp) {
                            viewModel.deleteArticle(article)
                        }
                        snackbar.dismiss()
                    }
                    snackbar.show()
                    snackbar.view.setOnClickListener { snackbar.dismiss() }
                    viewModel.selectedNewsListInHome.forEach {
                        it.isChecked = false
                    }
                    newsAdapter.notifyDataSetChanged()
                    viewModel.selectedNewsListInHome.clear()
                    viewModel.clearSelectedItemPositionListInHome()
                }
            }
            else {
                Toast.makeText(requireActivity(), "No News selected!!\nPlease Long press to select the news", Toast.LENGTH_SHORT).show()
            }
        }

        val navController: NavController = requireView().findNavController()
        return NavigationUI.onNavDestinationSelected(item, navController) ||
                super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        viewModel.selectedNewsListInHome.forEach { article ->
            article.isChecked = false
        }
        newsAdapter.notifyDataSetChanged()
        viewModel.selectedNewsListInHome.clear()
        viewModel.clearSelectedItemPositionListInHome()
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}