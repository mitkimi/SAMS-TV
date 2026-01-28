package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)
        
        val settingsButton = rootView.findViewById<Button>(R.id.settings_button)
        settingsButton?.setOnClickListener {
            openSettings()
        }
        
        return rootView
    }
    
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // Update UI after language change
        updateUI()
    }
    
    private fun updateUI() {
        // Update any UI elements if needed after language change
    }
    
    private fun openSettings() {
        val intent = Intent(activity, SettingsActivity::class.java)
        activity?.startActivity(intent)
    }
}