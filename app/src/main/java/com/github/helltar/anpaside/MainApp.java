package com.github.helltar.anpaside;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import com.github.helltar.anpaside.logging.RoboErrorReporter;

public class MainApp extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        RoboErrorReporter.bindReporter(this);
    }

    public static Context getContext() {
        return context;
    }

    public static String getStr(int resId) {
        return context.getString(resId);
    }
}
