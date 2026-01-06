package com.rajatt7z.retailx.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.rajatt7z.retailx.R
import java.util.Locale

open class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        
        // 1. Language Logic
        val language = sharedPreferences.getString("language", "English")
        val localeCode = when(language) {
            "Spanish" -> "es"
            "French" -> "fr"
            "German" -> "de"
            "Hindi" -> "hi"
            else -> "en"
        }
        val locale = Locale(localeCode)
        Locale.setDefault(locale)
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        // 2. Font Scale Logic (1.0f is default)
        // Slider 12-24 -> Map to 0.85 - 1.15 scale safely
        val fontSize = sharedPreferences.getFloat("font_size", 16f) 
        // 16sp is standard. 
        // 12sp -> 0.75
        // 24sp -> 1.5
        val fontScale = fontSize / 16.0f
        config.fontScale = fontScale

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        view?.let {
            applyGlobalFont(it)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyGlobalFont(window.decorView.findViewById(android.R.id.content))
    }

    private fun applyGlobalFont(rootView: View) {
        val sharedPreferences = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val fontName = sharedPreferences.getString("font_family", "Circular Spotify")
        
        val fontRes = when (fontName) {
            "Circular Std" -> R.font.circular_std
            "Montserrat Regular" -> R.font.montserrat_regular
            "Roboto Regular" -> R.font.roboto_regular
            else -> R.font.circular_spotify
        }

        try {
            val typeface = ResourcesCompat.getFont(this, fontRes)
            recursiveFontApply(rootView, typeface)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun recursiveFontApply(view: View, typeface: android.graphics.Typeface?) {
        if (view is TextView) {
            view.typeface = typeface
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                recursiveFontApply(view.getChildAt(i), typeface)
            }
        }
    }
}
