package com.haoutil.xposed.haoxperia.hook;

import android.preference.PreferenceScreen;

import com.haoutil.xposed.haoxperia.utils.Logger;
import com.haoutil.xposed.haoxperia.utils.SettingsHelper;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class CallHook extends BaseHook {
    public CallHook(SettingsHelper mSettingsHelper, Logger mLogger) {
        super(mSettingsHelper, mLogger);
    }

    @Override
    public void hookStartups(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    @Override
    public void hookResources(XC_InitPackageResources.InitPackageResourcesParam resParam) {
        if (resParam.packageName.equals("com.android.server.telecom")) {
            mSettingsHelper.reload();

            if (mSettingsHelper.getBoolean("pref_config_enable_call_blocking", false)) {
                resParam.res.setReplacement("com.android.server.telecom", "bool", "config_enable_call_blocking", true);
                mLogger.log("Show call blocking option");
            }
        }
    }

    @Override
    public void hookMethods(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("com.android.phone")) {
            mSettingsHelper.reload();
            if (mSettingsHelper.getBoolean("pref_config_enable_call_recording", false)) {
                Class clazz = XposedHelpers.findClass("com.android.phone.CallFeaturesSetting", loadPackageParam.classLoader);

                XposedHelpers.findAndHookMethod(clazz, "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if ((Boolean) XposedHelpers.callMethod(param.thisObject, "packageExists", "com.sonymobile.callrecording")) {
                            PreferenceScreen ps = (PreferenceScreen) XposedHelpers.callMethod(param.thisObject, "getPreferenceScreen");
                            if (ps.findPreference("call_recorder_listpreference_key") == null) {
                                XposedHelpers.callMethod(param.thisObject, "createCallRecorderSettings");
                            }
                            mLogger.log("Show call recording option");
                        }
                    }
                });
            }
        }
    }
}
