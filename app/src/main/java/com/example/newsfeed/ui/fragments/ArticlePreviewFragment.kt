package com.example.newsfeed.ui.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentArticlePreviewBinding
import com.example.newsfeed.entity.Article
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.viewmodel.NewsViewModel
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class ArticlePreviewFragment : Fragment() {

    private lateinit var binding: FragmentArticlePreviewBinding
    private lateinit var viewModel: NewsViewModel
    private lateinit var article: Article

    var clicked: Boolean = false

    private lateinit var fromBottom: Animation
    private lateinit var toBottom: Animation

    private val arguments: ArticlePreviewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        Log.i("ArticlePreviewFragment", "onCreateView")

        fromBottom = AnimationUtils.loadAnimation(requireActivity().applicationContext, R.anim.from_bottom_anim)
        toBottom = AnimationUtils.loadAnimation(requireActivity().applicationContext, R.anim.to_bottom_anim)

        binding = FragmentArticlePreviewBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        initUIElements()

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initUIElements() {
        article = if (arguments.isHomePageNews)
            viewModel.getArticleFromViewModelByTitle(arguments.articleTitle)!!
        else
            viewModel.getSavedArticleFromViewModelByTitle(arguments.articleTitle)!!

        binding.apply {
            textDetailTitle.text = article.title
            if (article.author.isNullOrBlank()) {
                textDetailAuthor.visibility = View.GONE
            }
            else textDetailAuthor.text = article.author!!.trim()
            if (article.publishedAt.isNullOrBlank()) {
                textDetailTime.visibility = View.GONE
            }
            else textDetailTime.text = parseTime(article.publishedAt)
            textDetailContent.text = article.content
            textDetailDetail.text = article.description
            if (article.urlToImage.isNullOrBlank()) {
                Picasso.get().load(R.drawable.news_logo_final).into(imgDetailNews)
            }
            else Picasso.get().load(article.urlToImage).into(imgDetailNews)
        }

        if (article.isExistInDB) {
            binding.bookmarkToggle.setImageResource(R.drawable.ic_baseline_bookmark_remove_24)
        }
        else {
            binding.bookmarkToggle.setImageResource(R.drawable.ic_baseline_bookmark_add_24)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.help.setOnClickListener {
            onAddButtonClicked()
        }

        binding.background.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.continueReading.setOnClickListener {
            val action = ArticlePreviewFragmentDirections.actionArticlePreviewFragmentToArticleFragment(arguments.articleTitle, arguments.isHomePageNews)
            requireView().findNavController().navigate(action)
        }

        binding.continueReadingButton.setOnClickListener {
            val action = ArticlePreviewFragmentDirections.actionArticlePreviewFragmentToArticleFragment(arguments.articleTitle, arguments.isHomePageNews)
            requireView().findNavController().navigate(action)
        }

        binding.shareButton.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, article.url)
            startActivity(shareIntent)
        }

        binding.bookmarkToggle.setOnClickListener {
            if (arguments.isHomePageNews) {
                if (article.isExistInDB) {
                    unbookmarkArticle(article)
                } else {
                    bookmarkArticle(article)
                }
            }
            else {
                if (!arguments.isHomePageNews) {
                    article.isExistInDB = false
                    viewModel.deleteArticle(article)
                    requireActivity().onBackPressed()
                    requireActivity().onBackPressed()
                }
                unbookmarkArticle(article)
            }
        }

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {

            context?.let {
                fromBottom = AnimationUtils.loadAnimation(context, R.anim.from_bottom_anim)
                toBottom = AnimationUtils.loadAnimation(context, R.anim.to_bottom_anim)
            }

            val scrollY: Int = binding.scrollView.scrollY // For ScrollView

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
        }
    }

    private fun parseTime(publishedTime: String?): String {
        val (date, time) = publishedTime?.split("T") ?: listOf("-1", "-1")
        if (date == "-1" || time == "-1") return "Not Found"

        val result = "$date, "

        val timeResult = time.dropLast(4)

        return "$result $timeResult UTC"
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        Log.i("ArticlePreviewFragment", "onViewStateRestored")
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
//            android:clickable="true"
//            android:focusable="true"
            binding.continueReading.visibility = View.VISIBLE
            binding.bookmarkToggle.visibility = View.VISIBLE
            binding.shareButton.visibility = View.VISIBLE
            binding.bookmarkToggleText.visibility = View.VISIBLE
            binding.continueReadingText.visibility = View.VISIBLE
            binding.shareButtonText.visibility = View.VISIBLE
        }
        else {
            binding.background.visibility = View.INVISIBLE
            binding.continueReading.visibility = View.INVISIBLE
            binding.bookmarkToggle.visibility = View.INVISIBLE
            binding.shareButton.visibility = View.INVISIBLE
            binding.bookmarkToggleText.visibility = View.INVISIBLE
            binding.continueReadingText.visibility = View.INVISIBLE
            binding.shareButtonText.visibility = View.INVISIBLE
        }
    }

    private fun setAnimation(clicked: Boolean) {
        val fromBottom: Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.from_bottom_anim)
        val toBottom: Animation = AnimationUtils.loadAnimation(requireActivity(), R.anim.to_bottom_anim)
        if (clicked) {
            binding.continueReading.animation = fromBottom
            binding.bookmarkToggle.animation = fromBottom
            binding.shareButton.animation = fromBottom
            binding.shareButtonText.animation = fromBottom
            binding.continueReadingText.animation = fromBottom
            binding.bookmarkToggleText.animation = fromBottom
        }
        else {
            binding.continueReading.animation = toBottom
            binding.bookmarkToggle.animation = toBottom
            binding.shareButton.animation = toBottom
            binding.shareButtonText.animation = toBottom
            binding.continueReadingText.animation = toBottom
            binding.bookmarkToggleText.animation = toBottom
        }
    }

    private fun setClickable(clicked: Boolean) {
        binding.continueReading.isClickable = clicked
        binding.bookmarkToggle.isClickable = clicked
        binding.shareButton.isClickable = clicked
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.i("ArticlePreviewFragment", "onAttach")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("ArticlePreviewFragment", "onCreate")
    }

    override fun onStart() {
        super.onStart()
        Log.i("ArticlePreviewFragment", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.i("ArticlePreviewFragment", "onResume")
    }

    override fun onPause() {
        super.onPause()
        clicked = false
        Log.i("ArticlePreviewFragment", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("ArticlePreviewFragment", "onStop")
    }

    override fun onDestroyView() {
        Log.i("ArticlePreviewFragment", "onDestroyView")
        super.onDestroyView()
        fromBottom.cancel()
        toBottom.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("ArticlePreviewFragment", "onDestroy")
    }

    override fun onDetach() {
        Log.i("ArticlePreviewFragment", "onDetach")
        super.onDetach()
        fromBottom.cancel()
        toBottom.cancel()
    }

}