package com.example.newsfeed.ui.fragments

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentChangeCountryBinding
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.viewmodel.NewsViewModel


class ChangeCountryFragment : Fragment() {

    private lateinit var binding: FragmentChangeCountryBinding
    private lateinit var viewModel: NewsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.i("ArticlePreviewFragment", "onCreateView")

        binding = FragmentChangeCountryBinding.inflate(layoutInflater)
        viewModel = (activity as MainActivity).viewModel

        setupDropDownAdapter()

//        binding.autoCompleteTextView.postDelayed({ binding.autoCompleteTextView.showDropDown() }, 500)
//        binding.autoCompleteTextView.setText("${map[sharedPref.getString("country", "india")]} - ${sharedPref.getString("country", "india")}")

        return binding.root
    }

    private fun setupDropDownAdapter() {
        val countries = resources.getStringArray(com.example.newsfeed.R.array.country_code_array)
        val arrayAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, countries)

        binding.autoCompleteTextView.setAdapter(arrayAdapter)
        binding.autoCompleteTextView.threshold = 1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.autoCompleteTextView.addTextChangedListener {
            binding.autoCompleteTextView.setTextColor(Color.BLACK)
        }

        binding.saveCountryButton.setOnClickListener {
            val countryNameWithCountryAbbr: String =
                binding.chooseCountryDropdown.editText!!.text.toString()

            if ("-" !in countryNameWithCountryAbbr) {
                for ((key, value) in viewModel.countryMap) {
                    if (countryNameWithCountryAbbr.trim().lowercase() == value.lowercase()) {
                        viewModel.saveNewsCountry(key)
//                        val action =
//                            ChangeCountryFragmentDirections.actionChangeCountryFragmentToHomeFragment()
//                        findNavController().navigate(action)
                        requireActivity().onBackPressed()
                        requireActivity().onBackPressed()
                    }
                }
                showError()
            } else {
                try {
                    val countryAbbr = countryNameWithCountryAbbr.split(" - ")[1]
                    if (countryAbbr.lowercase() !in viewModel.countryMap.keys) {
                        viewModel.saveNewsCountry(countryAbbr)
//                        val action =
//                            ChangeCountryFragmentDirections.actionChangeCountryFragmentToHomeFragment()
//                        findNavController().navigate(action)
                        requireActivity().onBackPressed()
                        requireActivity().onBackPressed()
                    } else
                        binding.autoCompleteTextView.setTextColor(Color.RED)
                } catch (e: Exception) {
                    showError()
                }
            }
        }
    }

    private fun showError() {
        binding.autoCompleteTextView.error =
            "Please select the valid country from the given options"
        binding.autoCompleteTextView.setTextColor(Color.RED)
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
        Log.i("ArticlePreviewFragment", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.i("ArticlePreviewFragment", "onStop")
    }

    override fun onDestroyView() {
        Log.i("ArticlePreviewFragment", "onDestroyView")
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("ArticlePreviewFragment", "onDestroy")
    }

    override fun onDetach() {
        Log.i("ArticlePreviewFragment", "onDetach")
        super.onDetach()
    }

}