package com.haoutil.xposed.haoxperia.activity;

import android.os.Bundle;

import com.haoutil.xposed.haoxperia.R;
import com.haoutil.xposed.haoxperia.fragment.SettingsFragment;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.container, new SettingsFragment()).commit();
        }
    }

    @Override
    public int getLayoutResource() {
        return R.layout.activity_settings;
    }
}
