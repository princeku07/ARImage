package com.xperiencelabs.arimage.screens.onBoarding

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.xperiencelabs.arimage.R
import com.xperiencelabs.arimage.databinding.FragmentBoardingBinding
import com.xperiencelabs.arimage.screens.onBoarding.screens.FirstScreen
import com.xperiencelabs.arimage.screens.onBoarding.screens.SecondScreen
import com.xperiencelabs.arimage.screens.onBoarding.screens.ThirdScreen


class BoardingFragment : Fragment() {
    private var _binding:FragmentBoardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = requireActivity().getSharedPreferences("isAppOpenedFirstTime", Context.MODE_PRIVATE)
        val isAppOpenedFirst = sharedPref?.getBoolean("isAppOpenedFirstTime",true)
        if (isAppOpenedFirst==false){
            findNavController().navigate(R.id.action_boardingFragment_to_homeFragment)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoardingBinding.inflate(layoutInflater)
        val fragmentList = arrayListOf<Fragment>(
            FirstScreen(),
            SecondScreen(),
            ThirdScreen()
        )
        binding.skip.setOnClickListener {
            findNavController().navigate(R.id.action_boardingFragment_to_homeFragment)
        }
        val adapter = ViewPagerAdapter(fragmentList,requireActivity().supportFragmentManager,lifecycle)
        binding.ViewPager.adapter = adapter
       return binding.root
    }



}