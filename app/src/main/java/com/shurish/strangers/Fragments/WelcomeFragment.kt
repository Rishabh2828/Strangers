package com.shurish.strangers.Fragments

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.shurish.strangers.R
import com.shurish.strangers.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private lateinit var binding : FragmentWelcomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentWelcomeBinding.inflate(layoutInflater)

        btnClicked()


        return binding.root
    }

    fun btnClicked(){
        binding.getStarted.setOnClickListener {
            findNavController().navigate(R.id.action_welcomeFragment_to_loginFragment)
        }
    }









}