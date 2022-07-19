package com.example.newsfeed.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.newsfeed.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class HelpFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val bottomNavBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_bar)
        bottomNavBar.visibility = View.GONE

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help, container, false)
    }

    override fun onDestroyView() {
        val bottomNavBar = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav_bar)
        bottomNavBar.visibility = View.VISIBLE
        super.onDestroyView()
    }

}