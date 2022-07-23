package com.example.newsfeed.ui

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.newsfeed.R
import com.example.newsfeed.databinding.ActivityMainBinding
import com.example.newsfeed.ui.fragments.ArticleFragment
import com.example.newsfeed.ui.fragments.ArticlePreviewFragment
import com.example.newsfeed.ui.fragments.BookmarksFragment
import com.example.newsfeed.ui.fragments.HomeFragment
import com.example.newsfeed.viewmodel.NewsViewModel
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
            viewModel.saveNewsCountry("in")
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
//        window.setBackgroundDrawable(
//            ResourcesCompat.getDrawable(
//                this.resources,
//                R.drawable.primary_gradient,
//                null
//            )
//        )

        // Here, we are setting taskbar
        window.statusBarColor = Color.rgb(55,71,79)
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
        viewModel = ViewModelProvider(this)[NewsViewModel::class.java]

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
                    (binding.customAppBar.myToolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP_MARGINS
                    (binding.fragmentContainerView.layoutParams as CoordinatorLayout.LayoutParams).behavior = null
                    (binding.fragmentContainerView.layoutParams as CoordinatorLayout.LayoutParams).topMargin = 220
                    hideSearchView()
                    hideBottomNavBar()
                    hideNavigationIcon()
                }
                R.id.homeFragment -> {
                    (binding.customAppBar.myToolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                        (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
                    (binding.fragmentContainerView.layoutParams as CoordinatorLayout.LayoutParams).behavior = AppBarLayout.ScrollingViewBehavior()
                    (binding.fragmentContainerView.layoutParams as CoordinatorLayout.LayoutParams).topMargin = 0

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
                    (binding.customAppBar.myToolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags =
                        (AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS)
                    (binding.fragmentContainerView.layoutParams as CoordinatorLayout.LayoutParams).behavior = AppBarLayout.ScrollingViewBehavior()
                    (binding.fragmentContainerView.layoutParams as CoordinatorLayout.LayoutParams).topMargin = 0

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
                R.id.aboutFragment -> {
                    hideNavigationIcon()
                    hideBottomNavBar()
                }
                R.id.helpFragment -> {
                    hideNavigationIcon()
                    hideBottomNavBar()
                }
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
            binding.customAppBar.searchView.setQuery("", false)
            viewModel.clearSearchQueryStack()
            viewModel.searchQuery = null
            homeFragment.clearAdapter()
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
            .setPositiveButton("Yes"
            ) { dialog, which -> finish() }
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
        }
        else if (navController.currentDestination!!.id == R.id.bookmarksFragment) {
            val bookmarksFragment =
                navHostFragment.childFragmentManager.fragments[0] as BookmarksFragment
            if (bookmarksFragment.isCheckboxEnable()) {
                bookmarksFragment.clearAdapterCheckboxes()
            }
            else if (viewModel.bookmarkSearchQuery != null) {
                bookmarksFragment.showAllBookmarks()
            }
            else
                super.onBackPressed()
        } else if (navController.currentDestination!!.id == R.id.articlePreviewFragment) {
            val articlePreviewFragment =
                navHostFragment.childFragmentManager.fragments[0] as ArticlePreviewFragment
            if (articlePreviewFragment.clicked) {
                articlePreviewFragment.onAddButtonClicked()
            } else
                super.onBackPressed()
        } else if (navController.currentDestination!!.id == R.id.articleFragment) {
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

    fun customizeSnackBar(snackbar: Snackbar) {
        val snackBarView = snackbar.view
        val params = snackBarView.layoutParams as CoordinatorLayout.LayoutParams

        params.setMargins(
            params.leftMargin,
            params.topMargin,
            params.rightMargin,
            params.bottomMargin + 160
        )

        snackBarView.layoutParams = params
    }
}