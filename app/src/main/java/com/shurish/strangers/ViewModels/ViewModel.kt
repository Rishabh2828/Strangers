package com.shurish.strangers.ViewModels


import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import com.shurish.strangers.User
import com.shurish.strangers.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ViewModel : androidx.lifecycle.ViewModel() {
    private val auth: FirebaseAuth = Utils.getAuthInstance()
    private val database: FirebaseDatabase = Utils.getDatabseInstance()

    private val _signedInSuccessfully = MutableStateFlow(false)
    val signedInSuccessfully: StateFlow<Boolean> = _signedInSuccessfully

    private val _coinValue = MutableStateFlow(0L)
    val coinValue: StateFlow<Long> = _coinValue

    private val _isACurrentUser = MutableStateFlow(false)
    val isACurrentUser: StateFlow<Boolean> = _isACurrentUser

    private val _profilePicUri = MutableLiveData<Uri?>()
    val profilePicUri: LiveData<Uri?> = _profilePicUri

    private val _incoming = MutableLiveData<String?>()
    val incoming: LiveData<String?> = _incoming

    private val _createdBy = MutableLiveData<String?>()
    val createdBy: LiveData<String?> = _createdBy

    private val _isAvailable = MutableLiveData<Boolean?>()
    val isAvailable: LiveData<Boolean?> = _isAvailable

    private val _changeFragment = MutableLiveData<Boolean?>()
    val changeFragment: LiveData<Boolean?> = _changeFragment

    val username: String? = auth.uid

    init {
        Utils.getAuthInstance().currentUser?.let {
            _isACurrentUser.value = true
        }
        _profilePicUri.value = auth.currentUser?.photoUrl
    }

    fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.getCurrentUser()
                    val firebaseUser = User(
                        user?.uid,
                        user?.displayName,
                        user?.photoUrl.toString(),
                        "Unknown",
                        500
                    )
                    user?.uid?.let {
                        database.reference
                            .child("profiles")
                            .child(it)
                            .setValue(firebaseUser).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    _signedInSuccessfully.value = true
                                }
                            }
                    }
                } else {
                    Log.e("err", task.exception?.localizedMessage ?: "Unknown error")
                }
            }
    }

    fun getCoins() {
        auth.currentUser?.let { user ->
            database.reference.child("profiles")
                .child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userSnapshot = snapshot.getValue(User::class.java)
                        val coins = userSnapshot?.coins ?: 0
                        _coinValue.value = coins
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ViewModel", "Error fetching coin value: ${error.message}")
                    }
                })
        }
    }

    fun updateCoins(coinsValue: Int) {
        database.reference.child("profiles")
            .child(auth.currentUser!!.getUid())
            .child("coins")
            .setValue(coinsValue)
    }

    fun getProfilePic() {
        val imageUri: Uri? = auth.currentUser?.photoUrl
        _profilePicUri.value = imageUri
    }

    fun signOut() {
        auth.signOut()
        _isACurrentUser.value = false
    }




    fun checkingRoom() {
        database.reference.child("users")
            .orderByChild("status")
            .equalTo(0.0)
            .limitToFirst(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.childrenCount > 0) {
                        val availableUserKey = snapshot.children.first().key
                        availableUserKey?.let { key ->
                            connectUsers(username!!, key)
                        }
                    } else {
                        createNewRoomForUser(username!!)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ViewModel", "Error checking room: ${error.message}")
                }
            })
    }





    private fun connectUsers(currentUserKey: String, availableUserKey: String) {
        val userRef = database.reference.child("users").child(availableUserKey)
        userRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val userStatus = mutableData.child("status").getValue(Int::class.java)
                if (userStatus == 1) {
                    // User is already connected, abort transaction
                    return Transaction.abort()
                }

                // User is available, connect them
                mutableData.child("incoming").value = currentUserKey
                mutableData.child("status").value = 1
                return Transaction.success(mutableData)
            }

            override fun onComplete(
                databaseError: DatabaseError?,
                committed: Boolean,
                dataSnapshot: DataSnapshot?
            ) {
                if (committed) {
                    // Transaction successful, update LiveData values
                    _incoming.value = currentUserKey
                    _createdBy.value = availableUserKey
                    _isAvailable.value = true
                    _changeFragment.value = true
                } else {
                    // Transaction aborted, user already connected
                    createNewRoomForUser(currentUserKey)
                }
            }
        })
    }




    private fun createNewRoomForUser(currentUserKey: String) {
        val room = HashMap<String, Any>()
        room["incoming"] = currentUserKey
        room["createdBy"] = currentUserKey
        room["isAvailable"] = true
        room["status"] = 0

        database.reference
            .child("users")
            .child(currentUserKey)
            .setValue(room)
            .addOnSuccessListener {
                _createdBy.value = currentUserKey
                database.reference
                    .child("users")
                    .child(currentUserKey)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.child("status").exists() &&
                                snapshot.child("status").getValue(Int::class.java) == 1
                            ) {
                                _incoming.value = snapshot.child("incoming").getValue(String::class.java)
                                _createdBy.value = snapshot.child("createdBy").getValue(String::class.java)
                                _isAvailable.value = snapshot.child("isAvailable").getValue(Boolean::class.java)
                                _changeFragment.value = true
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
    }






















//    fun checkingRoom() {
//        database.reference.child("users")
//            .orderByChild("status")
//            .equalTo(0.0).limitToFirst(1)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    if (snapshot.childrenCount > 0) {
//                        for (childSnap in snapshot.children) {
//                            database.reference
//                                .child("users")
//                                .child(childSnap.key!!)
//                                .child("incoming")
//                                .setValue(username)
//                            database.reference
//                                .child("users")
//                                .child(childSnap.key!!)
//                                .child("status")
//                                .setValue(1)
//
//                            _incoming.value = childSnap.child("incoming").getValue(String::class.java)
//                            _createdBy.value = childSnap.child("createdBy").getValue(String::class.java)
//                            _isAvailable.value = childSnap.child("isAvailable").getValue(Boolean::class.java)
//                            _changeFragment.value = true
//                        }
//                    } else {
//                        val room = HashMap<String, Any>()
//                        room["incoming"] = username!!
//                        room["createdBy"] = username!!
//                        room["isAvailable"] = true
//                        room["status"] = 0
//                        database.reference
//                            .child("users")
//                            .child(username!!)
//                            .setValue(room).addOnSuccessListener {
//                                _createdBy.value= username
//                                database.reference
//                                    .child("users")
//                                    .child(username!!)
//                                    .addValueEventListener(object : ValueEventListener {
//                                        override fun onDataChange(snapshot: DataSnapshot) {
//                                            if (snapshot.child("status").exists() &&
//                                                snapshot.child("status").getValue(Int::class.java) == 1
//                                            ) {
//                                                _incoming.value = snapshot.child("incoming").getValue(String::class.java)
//                                                _createdBy.value = snapshot.child("createdBy").getValue(String::class.java)
//                                                _isAvailable.value = snapshot.child("isAvailable").getValue(Boolean::class.java)
//                                                _changeFragment.value = true
//                                            }
//                                        }
//
//                                        override fun onCancelled(error: DatabaseError) {}
//                                    })
//                            }
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {}
//            })
//    }

    fun resetChangeFragmentState() {
        _changeFragment.value = null
    }
}