package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LanguageHelper {
    private const val PREF_LANGUAGE = "selected_language"
    
    enum class AppLanguage(val locale: String, val displayName: String) {
        SYSTEM("", "跟随系统"),
        CHINESE_SIMPLIFIED("zh-CN", "中文（简体）"),
        CHINESE_TRADITIONAL_HK("zh-HK", "繁体中文（香港）"),
        CHINESE_TRADITIONAL_TW("zh-TW", "繁体中文（台湾）"),
        ENGLISH("en", "英文")
    }
    
    fun setLocale(context: Context, languageCode: String) {
        val locale = when(languageCode) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-HK" -> Locale("zh", "HK")
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            else -> getSystemLocale()
        }
        
        updateResources(context, locale)
    }
    
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault()[0]
        } else {
            Locale.getDefault()
        }
    }
    
    private fun updateResources(context: Context, locale: Locale) {
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
    
    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
        return prefs.getString(PREF_LANGUAGE, "") ?: ""
    }
    
    fun saveSelectedLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences("TVAppPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_LANGUAGE, languageCode).apply()
    }
    
    fun getLocalizedContext(context: Context): Context {
        val languageCode = getSelectedLanguage(context)
        val locale = when(languageCode) {
            "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            "zh-HK" -> Locale("zh", "HK")
            "zh-TW" -> Locale.TRADITIONAL_CHINESE
            "en" -> Locale.ENGLISH
            else -> getSystemLocale()
        }
        
        val resources = context.resources
        val configuration = resources.configuration
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            configuration.setLocales(localeList)
            configuration.setLocale(locale)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
        } else {
            configuration.locale = locale
        }
        
        return context.createConfigurationContext(configuration)
    }
}