package com.example.newsfeed.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSettingsBinding.inflate(layoutInflater)

        requireActivity().title = "Settings"

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

        binding.personalisation.setOnClickListener {
            if (!binding.upDownArrowToggleButton.isChecked) {
                binding.upDownArrowToggleButton.isChecked = true
                binding.personalisationPopDown.visibility = View.VISIBLE
            }
            else {
                binding.upDownArrowToggleButton.isChecked = false
                binding.personalisationPopDown.visibility = View.GONE
            }
        }

        binding.changeCountry.setOnClickListener {

        }
    }

}