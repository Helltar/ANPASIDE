package com.github.helltar.anpaside;

import android.util.Log;

public class Logger {

    private static boolean DEBUG = true;

    // msgType: 0 - text, 1 - ok, 2 - error

    private static void addGuiLog(String msg, int msgType) {
        MainActivity.addGuiLog(msg, msgType);
    }

    public static void addLog(String msg) {
        addGuiLog(msg, 0);
    }

    public static void addLog(String msg, int msgType) {
        addGuiLog(msg, msgType);
    }

    public static void addLog(Exception e) {
        if (DEBUG) {
            Log.e("", e.getMessage());
        }

        addLog(e.getMessage(), 2);
    }
}

