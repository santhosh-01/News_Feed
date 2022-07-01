//package com.example.newsfeed.adapter
//
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.FragmentActivity
//import androidx.viewpager2.adapter.FragmentStateAdapter
//import com.example.newsfeed.ui.fragments.ArticleFragment
//import com.example.newsfeed.ui.fragments.ArticlePreviewFragment
//
//class ViewPagerAdapter(
//    fragmentActivity: FragmentActivity
//) : FragmentStateAdapter(fragmentActivity) {
//
//    private val fragmentArrayList: ArrayList<Fragment> = arrayListOf(ArticlePreviewFragment(), ArticleFragment())
//    private val fragmentTitle: ArrayList<String> = arrayListOf("Summary", "News Article")
//
//    override fun getItemCount(): Int {
//        return fragmentArrayList.size
//    }
//
//    override fun createFragment(position: Int): Fragment {
//        return fragmentArrayList[position]
//    }
//
//    fun addFragment(fragment: Fragment, title: String) {
//
//        fragmentArrayList.add(fragment)
//        fragmentTitle.add(title)
//
//    }
//
//}