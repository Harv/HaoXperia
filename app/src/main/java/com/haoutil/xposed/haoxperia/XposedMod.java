package com.haoutil.xposed.haoxperia;

import com.haoutil.xposed.haoxperia.hook.BaseHook;
import com.haoutil.xposed.haoxperia.hook.CallHook;
import com.haoutil.xposed.haoxperia.hook.LTEControllerHook;
import com.haoutil.xposed.haoxperia.hook.NetworkHook;
import com.haoutil.xposed.haoxperia.utils.Logger;
import com.haoutil.xposed.haoxperia.utils.SettingsHelper;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedMod implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private List<BaseHook> hooks;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        SettingsHelper mSettingsHelper = new SettingsHelper();
        Logger mLogger = new Logger(mSettingsHelper);

        hooks = new ArrayList<>();
        hooks.add(new NetworkHook(mSettingsHelper, mLogger));
        hooks.add(new LTEControllerHook(mSettingsHelper, mLogger));
        hooks.add(new CallHook(mSettingsHelper, mLogger));

        for (BaseHook hook : hooks) {
            hook.hookStartups(startupParam);
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resParam) throws Throwable {
        for (BaseHook hook : hooks) {
            hook.hookResources(resParam);
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        for (BaseHook hook : hooks) {
            hook.hookMethods(loadPackageParam);
        }
    }
}

