package com.shurish.strangers.Fragments

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shurish.strangers.R
import com.shurish.strangers.Utils
import com.shurish.strangers.ViewModels.ViewModel
import com.shurish.strangers.databinding.FragmentConnectingBinding
import kotlinx.coroutines.flow.MutableStateFlow


class ConnectingFragment : Fragment() {

    private lateinit var binding: FragmentConnectingBinding
    private val viewModel: ViewModel by viewModels()
    private var incoming :String? = null
    private var createdBy : String? = null
    private var isAvailable : Boolean = false



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentConnectingBinding.inflate(layoutInflater)

        observeViewModel()
        viewModel.checkingRoom()

        return binding.root
    }

    private fun observeViewModel() {
        viewModel.profilePicUri.observe(viewLifecycleOwner, Observer { uri ->
            uri?.let {
                Glide.with(this)
                    .load(it)
                    .into(binding.profile)
            }
        })

        viewModel.incoming.observe(viewLifecycleOwner, Observer {
            incoming = it
        })

        viewModel.createdBy.observe(viewLifecycleOwner, Observer {
            createdBy = it
        })

        viewModel.isAvailable.observe(viewLifecycleOwner, Observer {
            isAvailable == it
        })

        viewModel.changeFragment.observe(viewLifecycleOwner, Observer { shouldNavigate ->
            if (shouldNavigate == true) {
                navigateToCallingFragment()
                // Reset the changeFragment state in ViewModel
                viewModel.resetChangeFragmentState()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                showConfirmationDialog()
                true // Consumed the back press event
            } else {
                false
            }
        }
    }


     fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmation")
            .setMessage("Are you sure you want to go back?")
            .setPositiveButton("Yes") { dialog, _ ->
                Utils.getDatabseInstance().reference.child("users").child(createdBy!!).removeValue()
                dialog.dismiss()
                // Call onBackPressed in the activity if the user confirms
                requireActivity().onBackPressed()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun navigateToCallingFragment() {
        val bundle = Bundle().apply {
            putString("username", viewModel.username)
            putString("incoming", incoming)
            putString("createdBy", createdBy)
            putBoolean("isAvailable", isAvailable)
        }
        findNavController().navigate(R.id.action_connectingFragment_to_callingFragment, bundle)
    }








}