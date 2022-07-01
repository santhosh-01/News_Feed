package com.example.newsfeed.ui.fragments

import android.graphics.Color
import android.graphics.Color.rgb
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentCategoryBinding
import com.example.newsfeed.home.NewsViewModel
import com.example.newsfeed.ui.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class CategoryFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentCategoryBinding
    private lateinit var viewModel: NewsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_right_lined)

        requireActivity().title = "News Categories"

        binding = FragmentCategoryBinding.inflate(layoutInflater)

        binding.entertainment.setOnClickListener(this)
        binding.general.setOnClickListener(this)
        binding.business.setOnClickListener(this)
        binding.health.setOnClickListener(this)
        binding.science.setOnClickListener(this)
        binding.sports.setOnClickListener(this)
        binding.technology.setOnClickListener(this)

        viewModel = (activity as MainActivity).viewModel

        val selectedColor = Color.rgb(200,255,255)
        when(viewModel.selectedCategory) {
//            rgb(119, 76, 216)
            "general" -> {
                binding.general.setCardBackgroundColor(selectedColor)
                onSelectedItemClick(binding.general)
            }
            "entertainment" -> {
                binding.entertainment.setCardBackgroundColor(selectedColor)
                onSelectedItemClick(binding.entertainment)
            }
            "business" -> {
                binding.business.setCardBackgroundColor(selectedColor)
                onSelectedItemClick(binding.business)
            }
            "health" -> {
                binding.health.setCardBackgroundColor(selectedColor)
                onSelectedItemClick(binding.health)
            }
            "science" -> {
                binding.science.setCardBackgroundColor(selectedColor)
                onSelectedItemClick(binding.science)
            }
            "sports" -> {
                binding.sports.setCardBackgroundColor(selectedColor)
                onSelectedItemClick(binding.sports)
            }
            "technology" -> {
                binding.technology.setCardBackgroundColor(selectedColor)
                onSelectedItemClick(binding.technology)
            }
        }

//        requireActivity().findViewById<TextView>(R.id.toolbar_title).text = "News Categories"

//        This is the way to check the SDK Version
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            return resources.getDrawable(id, context.getTheme());
//        } else {
//            return resources.getDrawable(id);
//        }


        // Inflate the layout for this fragment
        return binding.root
    }

    private fun onSelectedItemClick(v: View) {
        v.setOnClickListener {
            Toast.makeText(requireActivity(), "Please select different category!!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClick(v: View?) {
        val action = CategoryFragmentDirections.actionCategoryFragmentToHomeFragment(v!!.id)
        findNavController().navigate(action)
    }

}