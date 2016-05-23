package com.github.helltar.anpaside;

import android.util.Log;

public class Logger {

    private static boolean DEBUG = true;

    private static void addGuiLog(String msg) {
        MainActivity.addGuiLog(msg);
    }

    public static void addLog(String msg) {
        addGuiLog(msg);
    }

    public static void addLog(Exception e) {
        if (DEBUG) {
            Log.e("", e.getMessage());
        }

        addLog(e.getMessage());
    }
}

