package com.jamal2367.cpuinfooverlay.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import com.jamal2367.cpuinfooverlay.R

class HideSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var hideLeftOverlayPreference: CheckBoxPreference? = null
        private var hideCPUTemperaturePreference: CheckBoxPreference? = null
        private var hideAppMemoryPreference: CheckBoxPreference? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.activity_settings_hide, rootKey)

            hideLeftOverlayPreference = findPreference("hide_left_overlay_key")
            hideCPUTemperaturePreference = findPreference("pref_hide_cpu_temperature_key")
            hideAppMemoryPreference = findPreference("pref_hide_app_memory_usage_key")

            // Listener for hideLeftOverlayPreference
            hideLeftOverlayPreference?.setOnPreferenceChangeListener { _, newValue ->
                newValue as Boolean
                true
            }

            hideLeftOverlayPreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }

            hideCPUTemperaturePreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }

            hideAppMemoryPreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
}
