package com.shurish.strangers.Fragments

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.shurish.strangers.InterfaceKotlin
import com.shurish.strangers.R
import com.shurish.strangers.User
import com.shurish.strangers.databinding.FragmentCallingBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class CallingFragment : Fragment(), InterfaceKotlin {
    private lateinit var binding: FragmentCallingBinding
    private lateinit var uniqueId: String
    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseRef: DatabaseReference
    private var username: String? = null
    private var friendsUsername: String? = null
    private var createdBy: String? = null
    private var isPeerConnected = false
    private var isAudio = true
    private var isVideo = true
    private var pageExit = false
    private var isNavigating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCallingBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        firebaseRef = FirebaseDatabase.getInstance().reference.child("users")

        username = arguments?.getString("username")
        val incoming = arguments?.getString("incoming")
        createdBy = arguments?.getString("createdBy")

        friendsUsername = incoming

        binding.webView.clearCache(true)
        binding.webView.clearHistory()
        binding.webView.clearFormData()

        setupWebView()
        observeUserStatus()

        binding.micBtn.setOnClickListener {
            isAudio = !isAudio
            callJavaScriptFunction("javascript:toggleAudio(\"$isAudio\")")
            binding.micBtn.setImageResource(if (isAudio) R.drawable.btn_unmute_normal else R.drawable.btn_mute_normal)
        }

        binding.videoBtn.setOnClickListener {
            isVideo = !isVideo
            callJavaScriptFunction("javascript:toggleVideo(\"$isVideo\")")
            binding.videoBtn.setImageResource(if (isVideo) R.drawable.btn_video_normal else R.drawable.btn_video_muted)
        }

        binding.endCall.setOnClickListener {
            sendDisconnectSignal()
            firebaseRef.child(createdBy!!).child("status").setValue(0)
            navigateToMainFragment()
        }
    }

    private fun observeUserStatus() {
        firebaseRef.child(createdBy!!).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(Int::class.java)
                    if (status == 0 && !isNavigating) {
                        isNavigating = true // Prevent multiple navigations
                        sendDisconnectSignal()
                        navigateToMainFragment()

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled if needed
                }
            })
    }

    private fun navigateToMainFragment() {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(1000) // Delay to avoid IllegalStateException
            try {
                findNavController().navigate(R.id.action_callingFragment_to_mainFragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupWebView() {
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.resources)
                }
            }
        }

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.addJavascriptInterface(InterfaceKotlinImpl(this), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"
        binding.webView.loadUrl(filePath)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                initializePeer()
            }
        }
    }

    private fun initializePeer() {
        uniqueId = getUniqueId()
        callJavaScriptFunction("javascript:init(\"$uniqueId\")")
        Toast.makeText(requireContext(), "Peer initialized with ID: $uniqueId", Toast.LENGTH_SHORT).show()


        if (createdBy.equals(username, ignoreCase = true)) {
            if (pageExit)
                return
            firebaseRef.child(username!!).child("connId").setValue(uniqueId)
            firebaseRef.child(username!!).child("isAvailable").setValue(true)

            binding.loadingGroup.visibility = View.GONE
            binding.controls.visibility = View.VISIBLE

            FirebaseDatabase.getInstance().getReference()
                .child("profiles")
                .child(friendsUsername!!)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        user?.let {
                            Glide.with(requireContext()).load(user.profile)
                                .into(binding.profile)
                            binding.name.text = user.name
                            binding.city.text = user.city
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })

        } else {
            Handler().postDelayed({
                friendsUsername = createdBy
                FirebaseDatabase.getInstance().getReference()
                    .child("profiles")
                    .child(friendsUsername!!)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            user?.let {
                                Glide.with(requireContext()).load(user.profile)
                                    .into(binding.profile)
                                binding.name.text = user.name
                                binding.city.text = user.city
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })

                FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(friendsUsername!!)
                    .child("connId")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.value != null) {
                                sendCallRequest()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }, 3000)
        }
    }

    private fun sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(requireContext(), "Peer connection not established.", Toast.LENGTH_SHORT).show()
            return
        }
        listenConnId()
    }
    private fun listenConnId() {
        val context = context ?: return // Check if context is null
        firebaseRef.child(friendsUsername!!).child("connId").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connId = snapshot.getValue(String::class.java)
                if (!connId.isNullOrEmpty()) {
                    binding.loadingGroup.visibility = View.GONE
                    binding.controls.visibility = View.VISIBLE
                    callJavaScriptFunction("javascript:startCall(\"$connId\")")
                } else {
                    Toast.makeText(context, "Connection ID not available.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun callJavaScriptFunction(function: String) {
        binding.webView.post {
            binding.webView.evaluateJavascript(function) { result ->
                Log.d("JavaScript", "JavaScript function result: $result")
            }
        }
    }

    private fun getUniqueId(): String {
        return UUID.randomUUID().toString()
    }

    override fun onPeerConnected() {
        isPeerConnected = true
    }

    private fun sendDisconnectSignal() {
        callJavaScriptFunction("javascript:disconnect()")
    }

    override fun onDestroy() {
        super.onDestroy()
        pageExit = true
        binding.webView.destroy()
        firebaseRef.child(createdBy!!).removeValue()

    }
    override fun onPause() {
        super.onPause()
        binding.webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.onResume()
    }

}

class InterfaceKotlinImpl(private val callback: InterfaceKotlin) {
    @JavascriptInterface
    fun onPeerConnected() {
        callback.onPeerConnected()
    }


}
