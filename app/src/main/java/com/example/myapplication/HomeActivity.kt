package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class HomeActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }
    
    override fun attachBaseContext(newBase: android.content.Context?) {
        super.attachBaseContext(newBase)
    }
}