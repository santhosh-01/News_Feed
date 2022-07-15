package com.example.newsfeed.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentArticleBinding
import com.example.newsfeed.entity.Article
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.viewmodel.NewsViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ArticleFragment : Fragment() {

    private lateinit var binding: FragmentArticleBinding
    private lateinit var viewModel: NewsViewModel
    private var isHomePageNews = true

    private lateinit var fromBottom: Animation
    private lateinit var toBottom: Animation

    var clicked: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fromBottom = AnimationUtils.loadAnimation(requireActivity().applicationContext, R.anim.from_bottom_anim)
        toBottom = AnimationUtils.loadAnimation(requireActivity().applicationContext, R.anim.to_bottom_anim)

        requireActivity().title = "News Article"

        binding = FragmentArticleBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        val arguments = ArticleFragmentArgs.fromBundle(requireArguments())
        val article = arguments.article
        val url = arguments.article.url.toString()

        if (!arguments.isHomePageNews) {
            binding.bookmarkToggle.setImageResource(R.drawable.ic_baseline_bookmark_remove_24)
            isHomePageNews = false
        }

        if (article.isExistInDB)
            binding.bookmarkToggle.setImageResource(R.drawable.ic_baseline_bookmark_remove_24)

//        binding.webView.apply {
//            webViewClient = WebViewClient()
//            loadUrl(url)
//        }

        binding.webView.webViewClient = (object : WebViewClient() {
            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                binding.progressBarMiddle.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressBarMiddle.visibility = View.GONE
            }
        })

        binding.webView.loadUrl(url)

        binding.help.setOnClickListener {
            onAddButtonClicked()
        }

        binding.background.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, article.url)
            startActivity(shareIntent)
        }

        binding.bookmarkToggle.setOnClickListener {
            if (isHomePageNews) {
                isHomePageNews = if (article.isExistInDB) {
                    unbookmarkArticle(article)
                    true
                } else {
                    bookmarkArticle(article)
                    false
                }
            }
            else {
                if (!arguments.isHomePageNews) {
                    article.isExistInDB = false
                    viewModel.deleteArticle(article)
                    findNavController().navigate(
                        ArticleFragmentDirections.actionArticleFragmentToBookmarksFragment())
                }
                unbookmarkArticle(article)
                isHomePageNews = true
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.webView.viewTreeObserver.addOnScrollChangedListener(ViewTreeObserver.OnScrollChangedListener {

            context?.let {
                fromBottom = AnimationUtils.loadAnimation(context, R.anim.from_bottom_anim)
                toBottom = AnimationUtils.loadAnimation(context, R.anim.to_bottom_anim)
            }

            val scrollY: Int = binding.webView.scrollY // For ScrollView

            if (scrollY > 200) {
                toBottom.duration = 500
                binding.help.visibility = View.INVISIBLE
                binding.help.animation = toBottom
                binding.help.isClickable = false
            } else {
                fromBottom.duration = 500
                binding.help.visibility = View.VISIBLE
                binding.help.animation = fromBottom
                binding.help.isClickable = true
            }

            // For HorizontalScrollView
            // DO SOMETHING WITH THE SCROLL COORDINATES
        })
    }

    private fun bookmarkArticle(article: Article) {
        lifecycleScope.launch {
            article.id = viewModel.insertArticle(article).toInt()
            article.isExistInDB = true
        }
        binding.bookmarkToggle.setImageResource(R.drawable.ic_baseline_bookmark_remove_24)
        val snackbar = Snackbar.make(requireView(),"Article saved Successfully", Snackbar.LENGTH_SHORT)
        snackbar.show()
        snackbar.view.setOnClickListener { snackbar.dismiss() }
    }

    private fun unbookmarkArticle(article: Article) {
        article.isExistInDB = false
        viewModel.deleteArticle(article)
        binding.bookmarkToggle.setImageResource(R.drawable.ic_baseline_bookmark_add_24)
        val snackbar = Snackbar.make(requireView(),"Article was removed from bookmark Successfully", Snackbar.LENGTH_SHORT)
        snackbar.show()
        snackbar.view.setOnClickListener { snackbar.dismiss() }
    }

    fun onAddButtonClicked() {
        clicked = !clicked
        setVisibility(clicked)
        setAnimation(clicked)
        setClickable(clicked)
    }

    private fun setVisibility(clicked: Boolean) {
        if (clicked) {
            binding.background.visibility = View.VISIBLE
            binding.background.alpha = 0.9F
            binding.background.isClickable = true
            binding.background.isFocusable = true
            binding.bookmarkToggle.visibility = View.VISIBLE
            binding.shareButton.visibility = View.VISIBLE
            binding.bookmarkToggleText.visibility = View.VISIBLE
            binding.shareButtonText.visibility = View.VISIBLE
        }
        else {
            binding.background.visibility = View.GONE
            binding.bookmarkToggle.visibility = View.GONE
            binding.shareButton.visibility = View.GONE
            binding.bookmarkToggleText.visibility = View.INVISIBLE
            binding.shareButtonText.visibility = View.INVISIBLE
        }
    }

    private fun setAnimation(clicked: Boolean) {
        if (clicked) {
            binding.bookmarkToggle.animation = fromBottom
            binding.shareButton.animation = fromBottom
        }
        else {
            binding.bookmarkToggle.animation = toBottom
            binding.shareButton.animation = toBottom
        }
    }

    private fun setClickable(clicked: Boolean) {
        binding.bookmarkToggle.isClickable = clicked
        binding.shareButton.isClickable = clicked
    }

}