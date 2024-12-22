package com.dozingcatsoftware.bouncy;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

public class BouncyPreferences extends PreferenceActivity {

    private static boolean supportsMultitouch() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    private static boolean supportsHapticFeedback(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) {
                return false;
            }
        }
        return true;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getWindow().setNavigationBarColor(Color.BLACK);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            this.getListView().setOnApplyWindowInsetsListener(this::applyWindowInsets);
        }

        // If multitouch or haptic feedback APIs aren't available, disable the preference items.
        if (!supportsMultitouch()) {
            CheckBoxPreference mtPref = (CheckBoxPreference) findPreference("independentFlippers");
            mtPref.setChecked(false);
            mtPref.setEnabled(false);
        }
        if (!supportsHapticFeedback(this)) {
            CheckBoxPreference hapticPref = (CheckBoxPreference) findPreference("haptic");
            hapticPref.setChecked(false);
            hapticPref.setEnabled(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.R)
    WindowInsets applyWindowInsets(View v, WindowInsets windowInsets) {
        Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        // android.util.Log.i("Preferences", "insets: " + insets);
        v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

        return WindowInsets.CONSUMED;
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Return to main activity when "P" is pressed, to avoid accidentally quitting by hitting
        // "back" multiple times. See https://github.com/dozingcat/Vector-Pinball/issues/103.
        if (keyCode == KeyEvent.KEYCODE_P) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
