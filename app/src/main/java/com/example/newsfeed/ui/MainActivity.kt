package com.example.newsfeed.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.newsfeed.databinding.ActivityMainBinding
import com.example.newsfeed.db.ArticleDatabase
import com.example.newsfeed.home.HomeFragment
import com.example.newsfeed.home.NewsViewModel
import com.example.newsfeed.home.NewsViewModelFactory
import com.example.newsfeed.repository.NewsRepository
import com.example.newsfeed.ui.fragments.ArticleFragment
import com.example.newsfeed.ui.fragments.ArticlePreviewFragment
import com.example.newsfeed.ui.fragments.BookmarksFragment


class MainActivity : AppCompatActivity() {

    private lateinit var myToolbar: Toolbar
    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: NewsViewModel
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Here, we are setting the complete background including status bas as primary_gradient
        window.setBackgroundDrawable(ResourcesCompat.getDrawable(this.resources, com.example.newsfeed.R.drawable.primary_gradient, null))

//        window.statusBarColor = ContextCompat.getColor(this, R.color.primaryGradientColor)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myToolbar = findViewById(com.example.newsfeed.R.id.my_toolbar)
        myToolbar.setNavigationIcon(com.example.newsfeed.R.drawable.ic_menu_left_lined)
        setSupportActionBar(myToolbar)

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val newsRepository = NewsRepository(ArticleDatabase.getInstance(this))
        val newsViewModelFactory =  NewsViewModelFactory(application, newsRepository)
        viewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)

        navHostFragment = supportFragmentManager.findFragmentById(com.example.newsfeed.R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        // To specify top level navigation in Nav Graph
        appBarConfiguration = AppBarConfiguration(setOf(com.example.newsfeed.R.id.homeFragment,
            com.example.newsfeed.R.id.settingsFragment, com.example.newsfeed.R.id.bookmarksFragment))

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id) {
                com.example.newsfeed.R.id.articlePreviewFragment -> {
                    hideSearchView()
                    hideBottomNavBar()
                    binding.customAppBar.navigationIcon.visibility = View.GONE
                }
//                R.id.articleFragment -> hideBottomNavBar()
                com.example.newsfeed.R.id.homeFragment -> {
                    showSearchView()
                    showBottomNavBar()
                    binding.customAppBar.navigationIcon.visibility = View.VISIBLE
                    binding.customAppBar.backNavigationIcon.visibility = View.GONE
                }
                com.example.newsfeed.R.id.settingsFragment -> {
                    hideSearchView()
                    showBottomNavBar()
                    binding.customAppBar.navigationIcon.visibility = View.GONE
                    viewModel.selectedNewsListInHome.forEach {
                        it.isChecked = false
                    }
                    viewModel.clearSelectedItemsInHome()
                }
                com.example.newsfeed.R.id.bookmarksFragment -> {
                    showSearchView()
                    showBottomNavBar()
                    binding.customAppBar.navigationIcon.visibility = View.GONE
                    viewModel.selectedNewsListInHome.forEach {
                        it.isChecked = false
                    }
                    viewModel.clearSelectedItemsInHome()
                }
                com.example.newsfeed.R.id.categoryFragment -> {
                    hideSearchView()
                    hideBottomNavBar()
                    binding.customAppBar.navigationIcon.visibility = View.GONE
                    viewModel.selectedNewsListInHome.forEach {
                        it.isChecked = false
                    }
                    viewModel.clearSelectedItemsInHome()
                }
                com.example.newsfeed.R.id.aboutFragment -> binding.customAppBar.navigationIcon.visibility = View.GONE
                com.example.newsfeed.R.id.helpFragment -> binding.customAppBar.navigationIcon.visibility = View.GONE
                com.example.newsfeed.R.id.change_country -> {
                    hideBottomNavBar()
                }
            }
        }

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.bottomNavBar, navController)

        sharedPref = this.getSharedPreferences("application", Context.MODE_PRIVATE)

        if (!sharedPref.contains("category") || savedInstanceState == null) {
            val editor = sharedPref.edit()
            editor.putString("category", "general")
            editor.apply()
        }

        if (!sharedPref.contains("country")) {
            val editor = sharedPref.edit()
            editor.putString("country", "global")
            editor.apply()
        }
    }

    private fun showSearchView() {
        binding.customAppBar.searchView.visibility = View.VISIBLE
    }

    private fun hideSearchView() {
        binding.customAppBar.searchView.visibility = View.GONE
    }

    private fun showBottomNavBar() {
        binding.bottomNavBar.visibility = View.VISIBLE
    }

    private fun hideBottomNavBar() {
        binding.bottomNavBar.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        if (navController.currentDestination!!.id == com.example.newsfeed.R.id.categoryFragment)
            title = ""
        return NavigationUI.navigateUp(navController, appBarConfiguration) ||
                super.onSupportNavigateUp()
    }

    private fun popSearchQuery() {
        viewModel.clearSearchQueryStack()
        if (binding.customAppBar.searchView.query.isNotEmpty()) {
            val homeFragment = navHostFragment.childFragmentManager.fragments[0] as HomeFragment
//            homeFragment.hideSortAndFilterTab()
            homeFragment.setUpRecyclerView(mutableListOf())
            binding.customAppBar.searchView.setQuery("",false)
            viewModel.clearSearchQueryStack()
            viewModel.newSearchQuery = null
            viewModel.initAccordingToCurrentConfig()
//                    viewModel.isSearchOn = false
        }
        else {
            showExitAlert()
        }
    }

    private fun showExitAlert() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Are you sure?")
            .setMessage("Do you want to Exit?")
            .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    finish()
                }
            })
            .setNegativeButton("No", null)
            .setCancelable(false)

        val alert: AlertDialog = builder.create()
        alert.show()
    }

    override fun onBackPressed() {
        if (navController.currentDestination!!.id == navController.graph.startDestinationId) {
            val homeFragment = navHostFragment.childFragmentManager.fragments[0] as HomeFragment
            if (homeFragment.isCheckboxEnable()) {
                homeFragment.clearAdapterCheckboxes()
            }
            else if (viewModel.searchQueryStack.value!!.size <= 1) {
                popSearchQuery()
            }
            else {
                val editor = sharedPref.edit()
                editor.putString("sortBy", "relevancy")
                editor.apply()
                viewModel.popFromSearchQueryStack()
                binding.customAppBar.searchView.setQuery(viewModel.popFromSearchQueryStack(), true)
            }
        }
        else if (navController.currentDestination!!.id == com.example.newsfeed.R.id.bookmarksFragment) {
            val bookmarksFragment = navHostFragment.childFragmentManager.fragments[0] as BookmarksFragment
            if (bookmarksFragment.isCheckboxEnable()) {
                bookmarksFragment.clearAdapterCheckboxes()
            }
            else
                super.onBackPressed()
        }
        else if (navController.currentDestination!!.id == com.example.newsfeed.R.id.articlePreviewFragment) {
            val articlePreviewFragment = navHostFragment.childFragmentManager.fragments[0] as ArticlePreviewFragment
            if (articlePreviewFragment.clicked) {
                articlePreviewFragment.onAddButtonClicked()
            }
            else
                super.onBackPressed()
        }
        else if (navController.currentDestination!!.id == com.example.newsfeed.R.id.articleFragment) {
            val articleFragment = navHostFragment.childFragmentManager.fragments[0] as ArticleFragment
            if (articleFragment.clicked) {
                articleFragment.onAddButtonClicked()
            }
            else
                super.onBackPressed()
        }
        else {
            super.onBackPressed()
        }
    }

    /*private fun callSuperOnBackPressed() {
        super.onBackPressed()
    }*/

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onPause() {
        super.onPause()
        Log.i("MainActivity", "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("MainActivity", "onDestroy")
    }

    override fun onRestart() {
        super.onRestart()
        Log.i("MainActivity", "onRestart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("MainActivity", "onResume")
    }

    /*private fun instantiateFragments(inState: Bundle?) {
        val manager: FragmentManager = supportFragmentManager
        val transaction: FragmentTransaction = manager.beginTransaction()
        if (inState != null) {
            mMyFragment = manager.getFragment(inState, ArticlePreviewFragment.TAG) as ArticlePreviewFragment
        } else {
            mMyFragment = ArticlePreviewFragment()
            transaction.add(com.example.newsfeed.R.id.fragment, mMyFragment, MyFragment.TAG)
            transaction.commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        val articlePreviewFragment = navHostFragment.childFragmentManager.fragments[0] as ArticlePreviewFragment
        supportFragmentManager.putFragment(outState, ArticlePreviewFragment.TAG, articlePreviewFragment)
    }*/

}