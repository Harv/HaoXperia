package com.haoutil.xposed.haoxperia.fragment;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.haoutil.xposed.haoxperia.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);

        addPreferencesFromResource(R.xml.fragment_settings);

        findPreference("pref_preferred_network_mode_marshal").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((newValue + ",").matches("(\\d+,)*")) {
                    return true;
                } else {
                    Toast.makeText(getActivity(), R.string.pref_preferred_network_mode_marshal_error, Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        if (!packageExists("com.sonymobile.callrecording")) {
            SwitchPreference mCallRecordingSwitch = (SwitchPreference) findPreference("pref_config_enable_call_recording");
            mCallRecordingSwitch.setEnabled(false);
            mCallRecordingSwitch.setSummary(R.string.pref_config_enable_call_recording_summary);
        }
    }

    private boolean packageExists(String packageName) {
        try {
            getActivity().getPackageManager().getPackageInfo(packageName, 1);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
