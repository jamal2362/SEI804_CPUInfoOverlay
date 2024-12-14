package com.jamal2367.cpuinfooverlay.settings

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jamal2367.cpuinfooverlay.R

class AboutSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.activity_settings_about, rootKey)

            // Set up "App Version" preference
            val preferenceAppVersion = findPreference<Preference>("pref_version_key")
            val packageName = requireContext().packageName
            val packageInfo = requireContext().packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName

            preferenceAppVersion?.title = getString(R.string.app_name) + " $versionName"
            preferenceAppVersion?.summary = "https://github.com/jamal2362/SEI804_CPUInfoOverlay"
            preferenceAppVersion?.setIcon(R.drawable.ic_info_24dp)
            preferenceAppVersion?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), "❤", Toast.LENGTH_LONG).show()
                true
            }
        }
    }
}
