package com.github.helltar.anpaside;

import android.app.Application;
import android.content.Context;

public class MainApp extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }

    public static String getStr(int resId) {
        return context.getString(resId);
    }
}