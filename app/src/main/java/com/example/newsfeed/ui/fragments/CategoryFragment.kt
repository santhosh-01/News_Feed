package com.example.newsfeed.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentCategoryBinding
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.viewmodel.NewsViewModel

class CategoryFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentCategoryBinding
    private lateinit var viewModel: NewsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_right_lined)

        binding = FragmentCategoryBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        initUIElements()

        binding.entertainment.setOnClickListener(this)
        binding.general.setOnClickListener(this)
        binding.business.setOnClickListener(this)
        binding.health.setOnClickListener(this)
        binding.science.setOnClickListener(this)
        binding.sports.setOnClickListener(this)
        binding.technology.setOnClickListener(this)

//        This is the way to check the SDK Version
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return resources.getDrawable(id, context.getTheme());
//        } else {
//            return resources.getDrawable(id);
//        }


        // Inflate the layout for this fragment
        return binding.root
    }

    private fun initUIElements() {
        val selectedColor = Color.rgb(200, 255, 255)
        when (viewModel.getNewsCategoryFromSharedPref()) {
//            rgb(119, 76, 216)
            "general" -> {
                binding.general.setCardBackgroundColor(selectedColor)
            }
            "entertainment" -> {
                binding.entertainment.setCardBackgroundColor(selectedColor)
            }
            "business" -> {
                binding.business.setCardBackgroundColor(selectedColor)
            }
            "health" -> {
                binding.health.setCardBackgroundColor(selectedColor)
            }
            "science" -> {
                binding.science.setCardBackgroundColor(selectedColor)
            }
            "sports" -> {
                binding.sports.setCardBackgroundColor(selectedColor)
            }
            "technology" -> {
                binding.technology.setCardBackgroundColor(selectedColor)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.general -> viewModel.saveNewsCategory("general")
            R.id.entertainment -> viewModel.saveNewsCategory("entertainment")
            R.id.business -> viewModel.saveNewsCategory("business")
            R.id.health -> viewModel.saveNewsCategory("health")
            R.id.science -> viewModel.saveNewsCategory("science")
            R.id.sports -> viewModel.saveNewsCategory("sports")
            R.id.technology -> viewModel.saveNewsCategory("technology")
        }
        requireActivity().onBackPressed()
//        findNavController().navigate(CategoryFragmentDirections.actionCategoryFragmentToHomeFragment())
    }

}