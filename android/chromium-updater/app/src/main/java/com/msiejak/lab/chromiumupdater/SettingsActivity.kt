package com.msiejak.lab.chromiumupdater

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.slider.Slider
import com.msiejak.lab.chromiumupdater.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyIfAvailable(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val slider = binding.slider
        val checkbox = binding.bkdCheck
        val prefs = getSharedPreferences("shared_prefs", MODE_PRIVATE)
        if(!prefs.getBoolean("bkd_check_enabled", true)) {
            slider.isEnabled = false
            checkbox.isChecked = false
        }else {
            slider.isEnabled = true
            checkbox.isChecked = true
        }
        checkbox.setOnCheckedChangeListener { _,_ ->
            if(!checkbox.isChecked) {
                slider.isEnabled = false
                prefs.edit().putBoolean("bkd_check_enabled", false).commit()
            }else {
                slider.isEnabled = true
                prefs.edit().putBoolean("bkd_check_enabled", true).commit()
            }
        }

        slider.value = prefs.getFloat("bkd_check_interval", 45F)
        val currentValue = binding.currentValue
        val currentValueText = slider.value.toInt().toString() + " Minutes"
        currentValue.text = currentValueText
        slider.setLabelFormatter { value: Float -> "${value.toInt()} Minutes" }
        slider.addOnChangeListener(Slider.OnChangeListener { _: Slider?, value: Float, _: Boolean ->
            val text = "${value.toInt()} Minutes"
            currentValue.text = text
            prefs.edit().putFloat("bkd_check_interval", value).apply()
        })

    }
}