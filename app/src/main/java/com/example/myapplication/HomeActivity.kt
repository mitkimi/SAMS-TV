package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity

class HomeActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply language settings before onCreate
        LanguageHelper.getLocalizedContext(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }
    
    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase?.let { LanguageHelper.getLocalizedContext(it) })
    }
}