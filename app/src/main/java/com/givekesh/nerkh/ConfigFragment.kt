package com.givekesh.nerkh

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class ConfigFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.config, rootKey)
    }
}
