package com.example.newsfeed.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.newsfeed.R
import com.example.newsfeed.databinding.FragmentAboutBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class AboutFragment : Fragment() {

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        binding = FragmentAboutBinding.inflate(layoutInflater)

        return binding.root
    }

}