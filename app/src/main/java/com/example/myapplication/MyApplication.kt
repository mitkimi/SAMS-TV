package com.example.myapplication

import android.app.Application
import android.content.res.Configuration

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply language setting when app starts
        LanguageHelper.getLocalizedContext(this)
    }

    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base?.let { LanguageHelper.getLocalizedContext(it) })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle language changes
    }
}