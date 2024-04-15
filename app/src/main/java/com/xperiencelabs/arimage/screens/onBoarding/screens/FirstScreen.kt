package com.xperiencelabs.arimage.screens.onBoarding.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentFirstScreenBinding


class FirstScreen : Fragment() {
    private var _binding:FragmentFirstScreenBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstScreenBinding.inflate(layoutInflater)
        val viewPager = activity?.findViewById<ViewPager2>(R.id.ViewPager)
        binding.next.setOnClickListener {
            viewPager?.currentItem = 1
        }
        return binding.root
    }

}