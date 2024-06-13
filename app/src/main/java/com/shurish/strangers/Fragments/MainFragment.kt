package com.shurish.strangers.Fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shurish.strangers.R
import com.shurish.strangers.Utils
import com.shurish.strangers.ViewModels.ViewModel
import com.shurish.strangers.databinding.FragmentMainBinding
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private lateinit var binding : FragmentMainBinding
    private  val viewModel : ViewModel by viewModels()
    private val database: FirebaseDatabase = Utils.getDatabseInstance()
    private val auth: FirebaseAuth = Utils.getAuthInstance()
    var permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private val requestCode = 1
    var coinsValue : Int? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentMainBinding.inflate(layoutInflater)



        getCoins()
        showProfilePic()


        binding.findButton.setOnClickListener {
            if (isPermissionsGranted()) {
                coinsValue?.let { coins ->
                    if (coins > 5) {
                        coinsValue = coins - 5
                        viewModel.updateCoins(coinsValue!!)

                        findNavController().navigate(R.id.action_mainFragment_to_connectingFragment)

                    } else {

                        Utils.showToast(requireContext(), "Insufficient Coins")

                    }
                }
            } else {
                askPermissions()
            }
        }






        return binding.root
    }

    fun showProfilePic(){
        val image : Uri? = com.shurish.strangers.Utils.getAuthInstance().currentUser?.photoUrl
        Glide.with(this)
            .load(image)
            .into(binding.profilePicture)
    }

    fun getCoins(){
        viewModel.getCoins()

        lifecycleScope.launch {
            viewModel.coinValue.collect { coins ->
                binding.coins.text = coins.toString()
                coinsValue = binding.coins.text.toString().toIntOrNull()

            }
        }
    }

    fun askPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), permissions, requestCode)
    }

    private fun isPermissionsGranted(): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    requireActivity(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }
        return true
    }




}