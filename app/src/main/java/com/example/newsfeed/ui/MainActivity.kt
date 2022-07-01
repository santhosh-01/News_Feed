package com.example.newsfeed.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.newsfeed.R
import com.example.newsfeed.databinding.ActivityMainBinding
import com.example.newsfeed.db.ArticleDatabase
import com.example.newsfeed.home.NewsViewModel
import com.example.newsfeed.home.NewsViewModelFactory
import com.example.newsfeed.repository.NewsRepository
import com.google.android.material.appbar.AppBarLayout

class MainActivity : AppCompatActivity() {

    private lateinit var myToolbar: Toolbar
    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: NewsViewModel
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Here, we are setting the complete background including status bas as primary_gradient
        window.setBackgroundDrawable(ResourcesCompat.getDrawable(this.resources, R.drawable.primary_gradient, null))

//        window.statusBarColor = ContextCompat.getColor(this, R.color.primaryGradientColor)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        myToolbar = findViewById(R.id.my_toolbar)
        myToolbar.setNavigationIcon(R.drawable.ic_menu_left_lined)
        setSupportActionBar(myToolbar)

//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val newsRepository = NewsRepository(ArticleDatabase.getInstance(this))
        val newsViewModelFactory =  NewsViewModelFactory(application, newsRepository)
        viewModel = ViewModelProvider(this, newsViewModelFactory).get(NewsViewModel::class.java)

        val navHostFragment: NavHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        // To specify top level navigation in Nav Graph
        appBarConfiguration = AppBarConfiguration(setOf(R.id.homeFragment,R.id.settingsFragment,R.id.bookmarksFragment))

        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            when(destination.id) {
                R.id.articlePreviewFragment -> {
                    hideBottomNavBar()
                    binding.navigationIcon.visibility = View.GONE
                }
//                R.id.articleFragment -> hideBottomNavBar()
                R.id.homeFragment -> {
                    showBottomNavBar()
                    binding.navigationIcon.visibility = View.VISIBLE
                    binding.backNavigationIcon.visibility = View.GONE
                }
                R.id.settingsFragment -> {
                    showBottomNavBar()
                    binding.navigationIcon.visibility = View.GONE
                }
                R.id.bookmarksFragment -> {
                    showBottomNavBar()
                    binding.navigationIcon.visibility = View.GONE
                }
                R.id.categoryFragment -> {
                    hideBottomNavBar()
                    binding.navigationIcon.visibility = View.GONE
                }
                R.id.aboutFragment -> binding.navigationIcon.visibility = View.GONE
                R.id.helpFragment -> binding.navigationIcon.visibility = View.GONE
            }
        }

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.bottomNavBar, navController)

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

    override fun onBackPressed() {
        if (navController.currentDestination!!.id == navController.graph.startDestinationId) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle("Are you sure?")
                .setMessage("Do you want to Exit?")
                .setPositiveButton("Yes", object : DialogInterface.OnClickListener {
                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        callSuperOnBackPressed()
                    }
                })
                .setNegativeButton("No", null)
                .setCancelable(false)

            val alert: AlertDialog = builder.create()
            alert.show()
        }
        else
            super.onBackPressed()
    }

    private fun callSuperOnBackPressed() {
        super.onBackPressed()
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}