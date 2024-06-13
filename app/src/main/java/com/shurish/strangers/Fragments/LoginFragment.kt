package com.shurish.strangers.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.shurish.strangers.Activities.MainActivity
import com.shurish.strangers.R
import com.shurish.strangers.Utils
import com.shurish.strangers.ViewModels.ViewModel
import com.shurish.strangers.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class LoginFragment : Fragment() {

    private lateinit var binding : FragmentLoginBinding
    private  val viewModel : ViewModel by viewModels()
    private lateinit var auth : FirebaseAuth

    companion object {
        const val RC_SIGN_IN = 9001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater)
        lifecycleScope.launch {
            viewModel.isACurrentUser.collect{

                if (it) {
                    Utils.showToast(requireContext(), "Welcome Back")
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                }
            }
        }

        lifecycleScope.launch {

            viewModel.signedInSuccessfully.collect{
                if (it) {
                    Utils.showToast(requireContext(), "Signed in")
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                }
            }
        }


        binding.loginBtn.setOnClickListener {
            signIn()
        }

        return binding.root
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.signInWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Handle sign-in failure
            }
        }
    }




}
