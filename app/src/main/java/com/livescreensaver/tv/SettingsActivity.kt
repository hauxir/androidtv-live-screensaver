package com.livescreensaver.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment : LeanbackSettingsFragmentCompat() {
        override fun onPreferenceStartInitialScreen() {
            startPreferenceFragment(PrefsFragment())
        }

        override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
        ): Boolean = false

        override fun onPreferenceStartScreen(
            caller: PreferenceFragmentCompat,
            pref: PreferenceScreen
        ): Boolean {
            val fragment = PrefsFragment()
            val args = Bundle(1)
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
            fragment.arguments = args
            startPreferenceFragment(fragment)
            return true
        }
    }

    class PrefsFragment : LeanbackPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val context = preferenceManager.context
            val screen = preferenceManager.createPreferenceScreen(context)

            val videoUrlPref = EditTextPreference(context).apply {
                key = "video_url"
                title = "Stream URL"
                setDefaultValue(LiveScreensaverService.DEFAULT_VIDEO_URL)
                setOnBindEditTextListener { editText ->
                    editText.setSingleLine(false)
                    editText.setLines(3)
                }
                summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
                    preference.text?.takeIf { it.isNotEmpty() } ?: "Enter YouTube live stream or HLS URL (.m3u8)"
                }
            }

            screen.addPreference(videoUrlPref)
            preferenceScreen = screen
        }
    }
}
