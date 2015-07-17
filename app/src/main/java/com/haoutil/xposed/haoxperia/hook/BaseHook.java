package com.haoutil.xposed.haoxperia.hook;

import android.content.ContentResolver;
import android.provider.Settings;

import com.haoutil.xposed.haoxperia.utils.Logger;
import com.haoutil.xposed.haoxperia.utils.SettingsHelper;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public abstract class BaseHook {
    public SettingsHelper mSettingsHelper;
    public Logger mLogger;

    public static final int NETWORK_TYPE_2GONLY = 0;
    public static final int NETWORK_TYPE_3GONLY = 1;
    public static final int NETWORK_TYPE_LTE = 2;

    public BaseHook(SettingsHelper mSettingsHelper, Logger mLogger) {
        this.mSettingsHelper = mSettingsHelper;
        this.mLogger = mLogger;
    }

    public abstract void hookStartups(IXposedHookZygoteInit.StartupParam startupParam);

    public abstract void hookResources(XC_InitPackageResources.InitPackageResourcesParam resParam);

    public abstract void hookMethods(XC_LoadPackage.LoadPackageParam loadPackageParam);

    public boolean isNetWork(int state, int networkType) {
        switch (networkType) {
            case NETWORK_TYPE_2GONLY:
                return state == 1 || state == 5;
            case NETWORK_TYPE_3GONLY:
                return state == 2 || state == 6 || state == 13 || state == 14;
            case NETWORK_TYPE_LTE:
                return state > 7 && state < 13 || state == 20 || state == 19 || state == 17 || state == 15;
            default:
                return false;
        }
    }


    public int getNetWork(ContentResolver mContentResolver, int networkType) {
        int rtn = Settings.Global.getInt(mContentResolver, "preferred_network_mode", 0);

        mSettingsHelper.reload();
        String[] networks = mSettingsHelper.getString("pref_preferred_network_mode_marshal", "18,20").split(",");
        for (int i = networks.length - 1; i >= 0; i--) {
            int nw = Integer.parseInt(networks[i]);
            if (isNetWork(nw, networkType)) {
                rtn = nw;
                break;
            }
        }

        return rtn;
    }
}
