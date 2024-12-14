package com.jamal2367.cpuinfooverlay.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import com.jamal2367.cpuinfooverlay.R

class CornerSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var roundedCornerOverallLeftPreference: CheckBoxPreference? = null
        private var roundedCornerOverallRightPreference: CheckBoxPreference? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.activity_settings_corner, rootKey)

            roundedCornerOverallLeftPreference = findPreference("rounded_corner_overall_left_key")
            roundedCornerOverallRightPreference = findPreference("rounded_corner_overall_right_key")

            roundedCornerOverallLeftPreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }

            roundedCornerOverallRightPreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
}
