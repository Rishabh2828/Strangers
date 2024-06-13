package com.shurish.strangers.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.shurish.strangers.Fragments.ConnectingFragment
import com.shurish.strangers.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.connectingFragment)
        if (fragment is ConnectingFragment) {
            fragment.showConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

}