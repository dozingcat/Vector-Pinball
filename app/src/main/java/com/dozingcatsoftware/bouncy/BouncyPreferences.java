package com.dozingcatsoftware.bouncy;

import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;

public class BouncyPreferences extends PreferenceActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // If multitouch or haptic feedback APIs aren't available, disable the preference items.
        // Coincidentally, both require Froyo (API level 8).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            CheckBoxPreference mtPref = (CheckBoxPreference) findPreference("independentFlippers");
            mtPref.setChecked(false);
            mtPref.setEnabled(false);

            CheckBoxPreference hapticPref = (CheckBoxPreference) findPreference("haptic");
            hapticPref.setChecked(false);
            hapticPref.setEnabled(false);
        }
    }
}
