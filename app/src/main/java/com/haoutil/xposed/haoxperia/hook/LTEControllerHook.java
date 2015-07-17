package com.haoutil.xposed.haoxperia.hook;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import com.haoutil.xposed.haoxperia.utils.Logger;
import com.haoutil.xposed.haoxperia.utils.SettingsHelper;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class LTEControllerHook extends BaseHook {
    public LTEControllerHook(SettingsHelper mSettingsHelper, Logger mLogger) {
        super(mSettingsHelper, mLogger);
    }

    @Override
    public void hookStartups(IXposedHookZygoteInit.StartupParam startupParam) {
    }

    @Override
    public void hookResources(XC_InitPackageResources.InitPackageResourcesParam resParam) {
    }

    @Override
    public void hookMethods(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("com.android.systemui")) {
            Class clazz = XposedHelpers.findClass("com.sonymobile.systemui.statusbar.policy.LTEControllerImpl", loadPackageParam.classLoader);

            XposedHelpers.findAndHookMethod(clazz, "isLteNetworkAvailable", Context.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.thisObject != null) {
                        boolean rtn = false;

                        String[] networkModes = ((String) XposedHelpers.callMethod(param.thisObject, "getPreferredNetworks", param.args[0])).split(",");
                        for (int i = networkModes.length - 1; i >= 0; i--) {
                            if (isNetWork(Integer.valueOf(networkModes[i]), NETWORK_TYPE_LTE)) {
                                rtn = true;
                                break;
                            }
                        }
                        param.setResult(rtn);
                    }
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getNetwork", String.class, boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    int rtn = XposedHelpers.getIntField(param.thisObject, "mCurrentState");

                    String[] networkModes = ((String) param.args[0]).split(",");
                    boolean shouldLte = (Boolean) param.args[1];
                    for (int i = networkModes.length - 1; i >= 0; i--) {
                        int network = Integer.valueOf(networkModes[i]);
                        if (shouldLte && isNetWork(network, NETWORK_TYPE_LTE) || !shouldLte && (!isNetWork(network, NETWORK_TYPE_LTE))) {
                            rtn = network;
                            break;
                        }
                    }
                    param.setResult(rtn);
                }
            });

            XposedHelpers.findAndHookMethod(clazz, "getPreferredNetworkMode", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    int networkMode = Settings.Global.getInt(mContext.getContentResolver(), "preferred_network_mode", 18);
                    param.setResult(networkMode);
                }
            });

            XposedHelpers.findAndHookConstructor(clazz, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                    Context mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    ContentObserver mContentObserver = (ContentObserver) XposedHelpers.getObjectField(param.thisObject, "mContentObserver");
                    mContext.getContentResolver().unregisterContentObserver(mContentObserver);

                    mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("preferred_network_mode"), true, new ContentObserver(new Handler()) {
                        @Override
                        public void onChange(boolean selfChange) {
                            int i = (Integer) XposedHelpers.callMethod(param.thisObject, "getPreferredNetworkMode");
                            int mCurrentState = XposedHelpers.getIntField(param.thisObject, "mCurrentState");
                            XposedHelpers.callMethod(param.thisObject, "access$102", param.thisObject, i);
                            XposedHelpers.callMethod(param.thisObject, "lteSettingsChanged");
                            if (isNetWork(i, NETWORK_TYPE_LTE)) {
                                if (!isNetWork(mCurrentState, NETWORK_TYPE_LTE)) {
                                    XposedHelpers.callMethod(param.thisObject, "access$202", param.thisObject, mCurrentState);
                                    mLogger.log("Enable LTE, network mode changed from " + mCurrentState + " to " + i);
                                }
                                XposedHelpers.callMethod(param.thisObject, "access$302", param.thisObject, true);
                            } else {
                                if (isNetWork(mCurrentState, NETWORK_TYPE_LTE)) {
                                    XposedHelpers.callMethod(param.thisObject, "access$402", param.thisObject, mCurrentState);
                                    mLogger.log("Disable LTE, network mode changed from " + mCurrentState + " to " + i);
                                }
                                XposedHelpers.callMethod(param.thisObject, "access$302", param.thisObject, false);
                            }
                        }
                    });
                }
            });
        }
    }
}
