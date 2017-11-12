package com.dozingcatsoftware.bouncy;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MotionEvent;

public class BouncyPreferences extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If multitouch APIs aren't available, don't show preferences which require them.
        boolean hasMultitouch = false;
        try {
            MotionEvent.class.getField("ACTION_POINTER_INDEX_MASK");
            hasMultitouch = true;
        }
        catch(Exception ignored) {}

        addPreferencesFromResource(hasMultitouch ? R.xml.preferences : R.xml.preferences_nomultitouch);
    }
}
