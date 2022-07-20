package com.example.newsfeed.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newsfeed.databinding.FragmentSettingsBinding
import com.example.newsfeed.ui.MainActivity
import com.example.newsfeed.viewmodel.NewsViewModel

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var viewModel: NewsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentSettingsBinding.inflate(layoutInflater)

        viewModel = (activity as MainActivity).viewModel

        binding.selectedCountryName.text = viewModel.countryMap[viewModel.selectedCountry]
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