package com.haoutil.xposed.haoxperia.utils;

import de.robv.android.xposed.XposedBridge;

public class Logger {

    private SettingsHelper settingsHelper;

    public Logger(SettingsHelper settingsHelper) {
        this.settingsHelper = settingsHelper;
    }

    public void log(String msg) {
        settingsHelper.reload();
        if (settingsHelper.getBoolean("pref_logger_enable", false)) {
            XposedBridge.log("[HaoXperia]" + msg);
        }
    }
}
