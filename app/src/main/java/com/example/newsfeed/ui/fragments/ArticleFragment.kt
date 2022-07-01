package com.example.newsfeed.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentArticleBinding
import com.example.newsfeed.home.NewsViewModel
import com.example.newsfeed.ui.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ArticleFragment : Fragment() {

    private lateinit var binding: FragmentArticleBinding
    private lateinit var viewModel: NewsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        requireActivity().title = "News Article"

        binding = FragmentArticleBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        val arguments = ArticleFragmentArgs.fromBundle(requireArguments())
        val article = arguments.article
        val url = arguments.article.url.toString()

        if (!arguments.addBookmarkFlag) binding.floatingActionButton2.setImageResource(R.drawable.ic_baseline_bookmark_remove_24)

        binding.webView.apply {
            webViewClient = WebViewClient()
            loadUrl(url)
        }

        binding.floatingActionButton2.setOnClickListener {
            if (arguments.addBookmarkFlag) {
                lifecycleScope.launch {
                    viewModel.insertArticle(article)
                }
                val snackbar = Snackbar.make(requireView(),"Article saved Successfully", Snackbar.LENGTH_SHORT)
                snackbar.show()
                snackbar.view.setOnClickListener { snackbar.dismiss() }
            }
            else {
                viewModel.deleteArticle(article)
                val action = ArticleFragmentDirections.actionArticleFragmentToBookmarksFragment3()
                requireView().findNavController().navigate(action)
                val snackbar = Snackbar.make(requireView(),"Article was removed from bookmark Successfully", Snackbar.LENGTH_SHORT)
                snackbar.show()
                snackbar.view.setOnClickListener { snackbar.dismiss() }
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

}