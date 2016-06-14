package com.github.helltar.anpaside.logging;

import com.github.helltar.anpaside.MainActivity;
import com.github.helltar.anpaside.MainApp;

public class Logger {

    public static final enum LogMsgType {
        lmtText, lmtOk, lmtError;
    }

    private static void addGuiLog(String msg, LogMsgType msgType) {
        MainActivity.addGuiLog(msg, msgType);
    }

    public static void addLog(String msg) {
        addGuiLog(msg, LogMsgType.lmtText);
    }

    public static void addLog(String msg, LogMsgType msgType) {
        addGuiLog(msg, msgType);
    }

    public static void addLog(Exception e) {
        addLog(e.getMessage(), LogMsgType.lmtError);
        RoboErrorReporter.reportError(MainApp.getContext(), e);
    }
}

