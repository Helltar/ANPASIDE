package com.github.helltar.anpaside.logging;

import com.github.helltar.anpaside.MainActivity;
import com.github.helltar.anpaside.MainApp;

public class Logger {

    public static final int LMT_TEXT = 0;
    public static final int LMT_INFO = 1;
    public static final int LMT_ERROR = 2;

    private static void addGuiLog(String msg, int msgType) {
        MainActivity.addGuiLog(msg, msgType);
    }

    public static void addLog(String msg) {
        addGuiLog(msg, LMT_TEXT);
    }

    public static void addLog(int resId) {
        addGuiLog(MainApp.getString(resId), LMT_TEXT);
    }

    public static void addLog(String msg, int msgType) {
        addGuiLog(msg, msgType);
    }

    public static void addLog(Exception e) {
        addGuiLog(e.getMessage(), LMT_ERROR);
        RoboErrorReporter.reportError(MainApp.getContext(), e);
    }
}

