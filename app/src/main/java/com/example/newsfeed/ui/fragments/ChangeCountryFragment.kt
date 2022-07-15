package com.example.newsfeed.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.newsfeed.databinding.FragmentChangeCountryBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class ChangeCountryFragment: Fragment() {

    private lateinit var binding: FragmentChangeCountryBinding
    private lateinit var sharedPref: SharedPreferences
    private var map: HashMap<String, String> = hashMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val bottomNavBar = requireActivity().findViewById<BottomNavigationView>(com.example.newsfeed.R.id.bottom_nav_bar)
        bottomNavBar.visibility = View.GONE

        binding = FragmentChangeCountryBinding.inflate(layoutInflater)

        requireActivity().title = "Choose Your Country"

        val countries = resources.getStringArray(com.example.newsfeed.R.array.country_code_array)

        val arrayAdapter = ArrayAdapter(requireContext(), com.example.newsfeed.R.layout.dropdown_item, countries)

        binding.autoCompleteTextView.setAdapter(arrayAdapter)
        binding.autoCompleteTextView.threshold = 1

        binding.autoCompleteTextView.addTextChangedListener {
            binding.autoCompleteTextView.setTextColor(Color.BLACK)
        }

        sharedPref = requireActivity().getSharedPreferences("application", Context.MODE_PRIVATE)

        for (countryNameWithCountryAbbr in countries) {
            val (countryName, countryAbbr) = countryNameWithCountryAbbr.split(" - ")
            map[countryAbbr] = countryName
        }

        binding.autoCompleteTextView.postDelayed({ binding.autoCompleteTextView.showDropDown() }, 500)
        binding.autoCompleteTextView.setText("${map[sharedPref.getString("country", "india")]} - ${sharedPref.getString("country", "india")}")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.saveCountryButton.setOnClickListener {
            val countryNameWithCountryAbbr: String = binding.chooseCountryDropdown.editText!!.text.toString()

            if ("-" !in countryNameWithCountryAbbr) {
                for ((key, value) in map) {
                    if (countryNameWithCountryAbbr.trim().lowercase() == value.lowercase()) {
                        savePreferredCountry(key)
                        requireActivity().onBackPressed()
                        requireActivity().onBackPressed()
                    }
                }
                binding.autoCompleteTextView.setTextColor(Color.RED)
            }
            else {
                try {
                    val countryAbbr = countryNameWithCountryAbbr.split(" - ")[1]
                    if (countryAbbr.lowercase() !in map.keys) {
                        savePreferredCountry(countryAbbr)
                        requireActivity().onBackPressed()
                        requireActivity().onBackPressed()
                    }
                    else
                        binding.autoCompleteTextView.setTextColor(Color.RED)
                }
                catch (e: Exception) {
//                binding.autoCompleteTextView.error = "Please select the valid country from dropdown"
                    binding.autoCompleteTextView.setTextColor(Color.RED)
//                binding.autoCompleteTextView.setText("Please select the valid country from dropdown")
                }
            }
        }
    }

    private fun savePreferredCountry(countryAbbr : String) {
        val editor = sharedPref.edit()
        editor.putString("country", countryAbbr)
        editor.apply()
    }

}