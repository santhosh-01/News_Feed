package com.example.newsfeed.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentArticlePreviewBinding
import com.example.newsfeed.entity.Article
import com.example.newsfeed.home.NewsViewModel
import com.example.newsfeed.ui.MainActivity
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class ArticlePreviewFragment : Fragment() {

    private lateinit var binding: FragmentArticlePreviewBinding
    private lateinit var viewModel: NewsViewModel
    private lateinit var article: Article

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        requireActivity().title = "Article Preview"

        binding = FragmentArticlePreviewBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        val arguments = ArticlePreviewFragmentArgs.fromBundle(requireArguments())
        article = arguments.article

        if (!arguments.addBookmarkFlag) {
            binding.floatingActionButton.setImageResource(R.drawable.ic_baseline_bookmark_remove_24)
        }

        initUIElements()

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = ArticlePreviewFragmentArgs.fromBundle(requireArguments())

        binding.continueReading.setOnClickListener {
            val action = ArticlePreviewFragmentDirections.actionArticlePreviewFragmentToArticleFragment(article, arguments.addBookmarkFlag)
            requireView().findNavController().navigate(action)
        }

        binding.floatingActionButton.setOnClickListener {
            if (arguments.addBookmarkFlag) {
                lifecycleScope.launch {
                    viewModel.insertArticle(article)
                }
                binding.floatingActionButton.setImageResource(R.drawable.ic_baseline_bookmark_remove_24)
                val snackbar = Snackbar.make(requireView(),"Article saved Successfully", Snackbar.LENGTH_SHORT)
                snackbar.show()
                snackbar.view.setOnClickListener { snackbar.dismiss() }
            }
            else {
                viewModel.deleteArticle(article)
                val action = ArticlePreviewFragmentDirections.actionArticlePreviewFragmentToBookmarksFragment()
                requireView().findNavController().navigate(action)
                val snackbar = Snackbar.make(requireView(),"Article was removed from bookmark Successfully", Snackbar.LENGTH_SHORT)
                snackbar.show()
                snackbar.view.setOnClickListener { snackbar.dismiss() }
            }
        }
    }

    private fun initUIElements() {
        binding.apply {
            textDetailTitle.text = article.title
            if (article.author.isNullOrBlank()) {
                textDetailAuthor.visibility = View.GONE
            }
            else textDetailAuthor.text = article.author
            if (article.publishedAt.isNullOrBlank()) {
                textDetailTime.visibility = View.GONE
            }
            else textDetailTime.text = article.publishedAt
            textDetailContent.text = article.content
            textDetailDetail.text = article.description
            if (article.urlToImage.isNullOrBlank()) {
                Picasso.get().load(R.drawable.img_not_available).into(imgDetailNews)
            }
            else Picasso.get().load(article.urlToImage).into(imgDetailNews)
        }
    }

}