package com.example.newsfeed.ui.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newsfeed.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var sharedPref: SharedPreferences
    private var map: HashMap<String, String> = hashMapOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSettingsBinding.inflate(layoutInflater)

        sharedPref = requireActivity().getSharedPreferences("application", Context.MODE_PRIVATE)

        val countries = resources.getStringArray(com.example.newsfeed.R.array.country_code_array)
        for (countryNameWithCountryAbbr in countries) {
            val (countryName, countryAbbr) = countryNameWithCountryAbbr.split(" - ")
            map[countryAbbr] = countryName
        }

        binding.selectedCountryName.text = map[sharedPref.getString("country", "")]
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.aboutApp.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToAboutFragment2())
        }

        binding.help.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToHelpFragment2())
        }

        binding.changeCountry.setOnClickListener {
            findNavController().navigate(SettingsFragmentDirections.actionSettingsFragmentToChangeCountryFragment())
        }
    }
}