package com.uber.simplestore.sample;

import android.app.Application;
import android.os.StrictMode;

public class SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDeath().build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().build());
    }
}
