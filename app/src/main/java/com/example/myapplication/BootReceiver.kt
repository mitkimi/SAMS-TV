package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val sharedPreferences = context.getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
            val autoStartEnabled = sharedPreferences.getBoolean("auto_start", false)
            
            if (autoStartEnabled) {
                val homeIntent = Intent(context, HomeActivity::class.java)
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(homeIntent)
            }
        }
    }
}