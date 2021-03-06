package com.example.newsfeed.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.newsfeed.R
import com.example.newsfeed.databinding.ActivityMainBinding
import com.example.newsfeed.db.ArticleDatabase
import com.example.newsfeed.repository.NewsRepository
import com.example.newsfeed.ui.fragments.ArticleFragment
import com.example.newsfeed.ui.fragments.ArticlePreviewFragment
import com.example.newsfeed.ui.fragments.BookmarksFragment
import com.example.newsfeed.ui.fragments.HomeFragment
import com.example.newsfeed.viewmodel.NewsViewModel
import com.example.newsfeed.viewmodel.NewsViewModelFactory


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: NewsViewModel
    private lateinit var myToolbar: Toolbar
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i("MainActivity", "onCreate")

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initAllElements()

        if (savedInstanceState == null) {
            viewModel.saveNewsCategory("general")
        }
    }

    private fun initAllElements() {
        // window.statusBarColor = ContextCompat.getColor(this, R.color.primaryGradientColor)
        setWindowBackgroundDrawable()
        setupCustomToolbar()
        initializeAttributes()
        setupActionBarAndBottomNavBarWithNavController()
    }

    private fun setWindowBackgroundDrawable() {
        // Here, we are setting the complete background including status bas as primary_gradient
        window.setBackgroundDrawable(
            ResourcesCompat.getDrawable(
                this.resources,
                R.drawable.primary_gradient,
                null
            )
        )
    }

    private fun setupCustomToolbar() {
        myToolbar = findViewById(R.id.my_toolbar)
        myToolbar.setNavigationIcon(R.drawable.ic_menu_left_lined)
        setSupportActionBar(myToolbar)
    }

    private fun setupActionBarAndBottomNavBarWithNavController() {
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.bottomNavBar, navController)
    }

    private fun initializeAttributes() {
        // initializeViewModel
        val newsRepository = NewsRepository(ArticleDatabase.getInstance(this))
        val newsViewModelFactory = NewsViewModelFactory(application, newsRepository)
        viewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)

        // To specify top level navigation in Nav Graph
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.settingsFragment,
                R.id.bookmarksFragment
            )
        )

        // Define NavController
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
    }

    private val onDestinationChangedListener =
        NavController.OnDestinationChangedListener { controller, destination, arguments ->
            when (destination.id) {
                R.id.articlePreviewFragment -> {
                    hideSearchView()
                    hideBottomNavBar()
                    hideNavigationIcon()
                }
                R.id.homeFragment -> {
                    showSearchView()
                    showBottomNavBar()
                    showNavigationIcon()
                }
                R.id.settingsFragment -> {
                    hideSearchView()
                    showBottomNavBar()
                    hideNavigationIcon()
                    viewModel.unSelectAllArticles()
                }
                R.id.bookmarksFragment -> {
                    showSearchView()
                    showBottomNavBar()
                    hideNavigationIcon()
                    viewModel.unSelectAllArticles()
                }
                R.id.categoryFragment -> {
                    hideSearchView()
                    hideBottomNavBar()
                    hideNavigationIcon()
                    viewModel.unSelectAllArticles()
                }
                R.id.aboutFragment -> hideNavigationIcon()
                R.id.helpFragment -> hideNavigationIcon()
                R.id.change_country -> hideBottomNavBar()
            }
        }

    private fun showSearchView() {
        binding.customAppBar.searchView.visibility = View.VISIBLE
    }

    private fun hideSearchView() {
        binding.customAppBar.searchView.visibility = View.GONE
    }

    private fun showNavigationIcon() {
        binding.customAppBar.navigationIcon.visibility = View.VISIBLE
    }

    private fun hideNavigationIcon() {
        binding.customAppBar.navigationIcon.visibility = View.GONE
    }

    private fun showBottomNavBar() {
        binding.bottomNavBar.visibility = View.VISIBLE
    }

    private fun hideBottomNavBar() {
        binding.bottomNavBar.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        if (navController.currentDestination!!.id == R.id.categoryFragment)
            title = ""
        return NavigationUI.navigateUp(navController, appBarConfiguration) ||
                super.onSupportNavigateUp()
    }

    private fun popSearchQuery() {
        viewModel.clearSearchQueryStack()
        if (binding.customAppBar.searchView.query.isNotEmpty()) {
            val homeFragment = navHostFragment.childFragmentManager.fragments[0] as HomeFragment
            homeFragment.setUpRecyclerView(mutableListOf())
            binding.customAppBar.searchView.setQuery("", false)
            viewModel.clearSearchQueryStack()
            viewModel.newSearchQuery = null
            viewModel.initAccordingToCurrentConfig()
//                    viewModel.isSearchOn = false
        } else {
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
            } else if (viewModel.searchQueryStack.value!!.size <= 1) {
                popSearchQuery()
            } else {
                viewModel.popFromSearchQueryStack()
                binding.customAppBar.searchView.setQuery(viewModel.popFromSearchQueryStack(), true)
            }
        } else if (navController.currentDestination!!.id == R.id.bookmarksFragment) {
            val bookmarksFragment =
                navHostFragment.childFragmentManager.fragments[0] as BookmarksFragment
            if (bookmarksFragment.isCheckboxEnable()) {
                bookmarksFragment.clearAdapterCheckboxes()
            } else
                super.onBackPressed()
        } else if (navController.currentDestination!!.id == R.id.articlePreviewFragment) {
            val articlePreviewFragment =
                navHostFragment.childFragmentManager.fragments[0] as ArticlePreviewFragment
            if (articlePreviewFragment.clicked) {
                articlePreviewFragment.onAddButtonClicked()
            } else
                super.onBackPressed()
        } else if (navController.currentDestination!!.id == com.example.newsfeed.R.id.articleFragment) {
            val articleFragment =
                navHostFragment.childFragmentManager.fragments[0] as ArticleFragment
            if (articleFragment.clicked) {
                articleFragment.onAddButtonClicked()
            } else
                super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("MainActivity", "onResume")
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

    override fun onStop() {
        super.onStop()
        Log.i("MainActivity", "onStop")
    }
}