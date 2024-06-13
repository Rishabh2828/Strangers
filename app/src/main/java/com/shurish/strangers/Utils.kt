package com.shurish.strangers

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object Utils {

    fun showToast(context: Context, message : String){
        Toast.makeText(context,message, Toast.LENGTH_SHORT).show()
    }


    private var firebaseauthInstance : FirebaseAuth? = null
    fun getAuthInstance() : FirebaseAuth{
          if (firebaseauthInstance == null){
              firebaseauthInstance = FirebaseAuth.getInstance()
          }

        return  firebaseauthInstance!!
    }

    private var firebaseDatabseInstance : FirebaseDatabase? = null
    fun getDatabseInstance() : FirebaseDatabase{
        if (firebaseDatabseInstance == null){
            firebaseDatabseInstance = FirebaseDatabase.getInstance()
        }

        return  firebaseDatabseInstance!!
    }


}