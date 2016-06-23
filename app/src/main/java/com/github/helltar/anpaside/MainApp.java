package com.github.helltar.anpaside;

import android.app.Application;
import android.content.Context;
import com.github.helltar.anpaside.logging.RoboErrorReporter;

public class MainApp extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        RoboErrorReporter.bindReporter(this);
    }

    public static Context getContext() {
        return context;
    }

    public static String getString(int resId) {
        return context.getString(resId);
    }
}

