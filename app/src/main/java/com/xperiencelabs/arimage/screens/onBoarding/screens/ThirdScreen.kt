package com.xperiencelabs.arimage.screens.onBoarding.screens

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentThirdScreenBinding
import com.xperiencelabs.arimage.utils.makeToast


class ThirdScreen : Fragment() {
   private var _binding:FragmentThirdScreenBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThirdScreenBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.video.playAnimation()
        binding.finish.setOnClickListener {
            findNavController().navigate(R.id.action_boardingFragment_to_homeFragment)
        }
    }

}