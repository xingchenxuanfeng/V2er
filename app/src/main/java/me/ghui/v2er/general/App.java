package me.ghui.v2er.general;

import android.app.Application;
import android.preference.PreferenceManager;

import com.orhanobut.logger.Logger;
import com.tencent.bugly.crashreport.CrashReport;

import me.ghui.v2er.BuildConfig;
import me.ghui.v2er.R;
import me.ghui.v2er.injector.component.AppComponent;
import me.ghui.v2er.injector.component.DaggerAppComponent;
import me.ghui.v2er.injector.module.AppModule;
import me.ghui.v2er.network.APIService;
import me.ghui.v2er.util.UserUtils;

/**
 * Created by ghui on 05/03/2017.
 */

public class App extends Application {

    private static App sInstance;
    private AppComponent mAppComponent;

    public static App get() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    private void init() {
        sInstance = this;
        mAppComponent = DaggerAppComponent.builder().appModule(new AppModule(sInstance))
                .build();
        APIService.init();
        Logger.init().methodCount(1).hideThreadInfo();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        initBugly();
    }

    private void initBugly() {
        CrashReport.initCrashReport(getApplicationContext(), "6af3e083ba", BuildConfig.DEBUG);
        if (UserUtils.isLogin()) {
            CrashReport.setUserId(UserUtils.getUserInfo().getUserName());
        } else {
            CrashReport.setUserId("UnLogin");
        }
    }

    public AppComponent getAppComponent() {
        return mAppComponent;
    }

}
