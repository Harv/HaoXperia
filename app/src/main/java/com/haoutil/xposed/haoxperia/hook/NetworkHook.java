package com.haoutil.xposed.haoxperia.hook;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import com.haoutil.xposed.haoxperia.utils.Constant;
import com.haoutil.xposed.haoxperia.utils.Logger;
import com.haoutil.xposed.haoxperia.utils.SettingsHelper;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class NetworkHook extends BaseHook {
    private Object mMobileNetworkSettings;
    private ListPreference mButtonPreferredNetworkMode;
    private SwitchPreference mButtonUse2gOnly;
    private SwitchPreference mButtonUse3gOnly;
    private Handler mHandler;
    private Object mPhone;
    private ContentResolver mContentResolver;

    public NetworkHook(SettingsHelper mSettingsHelper, Logger mLogger) {
        super(mSettingsHelper, mLogger);
    }

    @Override
    public void hookStartups(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    @Override
    public void hookResources(XC_InitPackageResources.InitPackageResourcesParam resParam) {
        if (resParam.packageName.equals("com.android.phone")) {
            mSettingsHelper.reload();

            resParam.res.setReplacement("com.android.phone", "array", "clh_preferred_network_mode_choices", Constant.NETWORK_MODE_CHOICES);
            resParam.res.setReplacement("com.android.phone", "array", "clh_preferred_network_mode_values", Constant.NETWORK_MODE_VALUES);

            String preferred_network_mode_marshal = mSettingsHelper.getString("pref_preferred_network_mode_marshal", "18,20");
            resParam.res.setReplacement("com.android.phone", "string", "preferred_network_mode_marshal", preferred_network_mode_marshal);
            mLogger.log("Replace preferred network mode marshal with " + preferred_network_mode_marshal);

            if (mSettingsHelper.getBoolean("pref_prefer_2g_visibility", false)) {
                resParam.res.setReplacement("com.android.phone", "bool", "prefer_2g_visibility", true);
                mLogger.log("Show use 2g only option");
            }

            if (mSettingsHelper.getBoolean("pref_use_3g_only", false)) {
                resParam.res.setReplacement("com.android.phone", "bool", "use_3g_only", true);
                mLogger.log("Show use 3g only option");
            }
        } else if (resParam.packageName.equals("com.android.systemui")) {
            mSettingsHelper.reload();

            if (mSettingsHelper.getBoolean("pref_config_show4GForLTE", false)) {
                resParam.res.setReplacement("com.android.systemui", "bool", "config_show4GForLTE", true);
                mLogger.log("Show 4G For LTE");
            }
        }
    }

    @Override
    public void hookMethods(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("com.android.phone")) {
            Class clazz = XposedHelpers.findClass("com.android.phone.MobileNetworkSettings", loadPackageParam.classLoader);

            XposedHelpers.findAndHookMethod(clazz, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    mMobileNetworkSettings = param.thisObject;
                    mButtonPreferredNetworkMode = (ListPreference) XposedHelpers.getObjectField(param.thisObject, "mButtonPreferredNetworkMode");
                    mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");
                    mPhone = XposedHelpers.getObjectField(param.thisObject, "mPhone");
                    mContentResolver = ((Context) XposedHelpers.callMethod(mPhone, "getContext")).getContentResolver();
                    try {
                        mButtonUse2gOnly = (SwitchPreference) XposedHelpers.getObjectField(param.thisObject, "mButtonUse2gOnly");
                    } catch (Throwable t) {
                        mButtonUse2gOnly = null;
                    }
                    try {
                        mButtonUse3gOnly = (SwitchPreference) XposedHelpers.getObjectField(param.thisObject, "mButtonUse3gOnly");
                    } catch (Throwable t) {
                        mButtonUse3gOnly = null;
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "UpdatePreferredNetworkModeSummary", int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int choice = (Integer) param.args[0];
                    if (choice >= 13) {
                        mButtonPreferredNetworkMode.setSummary(Constant.NETWORK_MODE_CHOICES[choice]);
                        mLogger.log("Change preferred network mode summary to " + Constant.NETWORK_MODE_CHOICES[choice]);
                        param.setResult(null);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "onPreferenceChange", Preference.class, Object.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[0] == mButtonPreferredNetworkMode) {
                        int networkMode = Integer.valueOf((String) param.args[1]);
                        if (networkMode >= 13) {
                            int settingsNetworkMode = Settings.Global.getInt(mContentResolver, "preferred_network_mode", 0);
                            if (networkMode != settingsNetworkMode) {
                                XposedHelpers.callMethod(mPhone, "setPreferredNetworkType", networkMode, mHandler.obtainMessage(1));
                                Settings.Global.putInt(mContentResolver, "preferred_network_mode", networkMode);
                                XposedHelpers.callMethod(mMobileNetworkSettings, "UpdatePreferredNetworkModeSummary", networkMode);
                                mLogger.log("Change preferred network mode to " + settingsNetworkMode);
                            }

                            param.setResult(true);
                        }
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "onPreferenceTreeClick", PreferenceScreen.class, Preference.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[1] == mButtonUse2gOnly) {
                        int j = getNetWork(NETWORK_TYPE_LTE);
                        if (mButtonUse2gOnly.isChecked()) {
                            j = getNetWork(NETWORK_TYPE_2GONLY);
                        }

                        Settings.Global.putInt(mContentResolver, "preferred_network_mode", j);
                        XposedHelpers.callMethod(mPhone, "setPreferredNetworkType", j, mHandler.obtainMessage(1));
                        mLogger.log((mButtonUse2gOnly.isChecked() ? "Use 2g only" : "Don't use 2g only") + ", change preferred network mode to " + j);

                        param.setResult(true);
                    } else if (param.args[1] == mButtonUse3gOnly) {
                        int i = getNetWork(NETWORK_TYPE_LTE);
                        if (mButtonUse3gOnly.isChecked()) {
                            i = getNetWork(NETWORK_TYPE_3GONLY);
                        }

                        Settings.Global.putInt(mContentResolver, "preferred_network_mode", i);
                        XposedHelpers.callMethod(mPhone, "setPreferredNetworkType", i, mHandler.obtainMessage(1));
                        mLogger.log((mButtonUse3gOnly.isChecked() ? "Use 3g only" : "Don't use 3g only") + ", change preferred network mode to " + i);

                        param.setResult(true);
                    }
                }
            });

            XposedHelpers.findAndHookMethod("com.android.phone.MobileNetworkSettings$MyHandler", loadPackageParam.classLoader, "handleGetPreferredNetworkTypeResponse", Message.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Object asyncResult = ((Message) param.args[0]).obj;
                    if (XposedHelpers.getObjectField(asyncResult, "exception") == null) {
                        int networkMode = ((int[]) XposedHelpers.getObjectField(asyncResult, "result"))[0];
                        if (networkMode >= 13) {
                            int settingsNetworkMode = Settings.Global.getInt(mContentResolver, "preferred_network_mode", 0);
                            if (networkMode != settingsNetworkMode) {
                                XposedHelpers.callMethod(mPhone, "setPreferredNetworkType", networkMode, mHandler.obtainMessage(1));
                            }
                            Settings.Global.putInt(mContentResolver, "preferred_network_mode", networkMode);
                            XposedHelpers.callMethod(mButtonPreferredNetworkMode, "setValue", Integer.toString(networkMode));
                            XposedHelpers.callMethod(mMobileNetworkSettings, "UpdatePreferredNetworkModeSummary", networkMode);
                            mLogger.log("Change preferred network mode(handler) to " + settingsNetworkMode);

                            if (mButtonUse2gOnly != null) {
                                mButtonUse2gOnly.setChecked(isNetWork(networkMode, NETWORK_TYPE_2GONLY));
                            }

                            if (mButtonUse3gOnly != null) {
                                mButtonUse3gOnly.setChecked(isNetWork(networkMode, NETWORK_TYPE_3GONLY));
                            }

                            param.setResult(null);
                        }
                    }
                }
            });
        }
    }

    public int getNetWork(int networkType) {
        return getNetWork(mContentResolver, networkType);
    }
}
