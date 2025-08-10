package org.eu.hanana.reimu.ottohub_app_web.frag;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import org.eu.hanana.reimu.ottohub_app_web.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference, rootKey);
    }
}