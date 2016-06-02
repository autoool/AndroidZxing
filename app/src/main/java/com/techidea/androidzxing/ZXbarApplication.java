package com.techidea.androidzxing;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by zchao on 2016/6/2.
 */
public class ZXbarApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            LeakCanary.install(this);
        }
        initialize();
    }

    private void initialize() {
        if (BuildConfig.DEBUG) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                        .detectAll().penaltyLog().build());
                StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                        .detectAll().penaltyLog().penaltyDeathOnNetwork().build());
            }
        }
    }
}
