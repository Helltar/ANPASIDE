package com.github.helltar.anpaside.logging;

import android.text.Html;
import android.text.Spanned;

import com.github.helltar.anpaside.MainActivity;
import com.github.helltar.anpaside.MainApp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    public static final int LMT_TEXT = 0;
    public static final int LMT_INFO = 1;
    public static final int LMT_ERROR = 2;

    private static void addGuiLog(String msg, int msgType) {
        if (msg.isEmpty()) {
            return;
        }

        String fontColor = "#aaaaaa";

        if (msgType == LMT_INFO) {
            fontColor = "#00aa00";
        } else if (msgType == LMT_ERROR) {
            fontColor = "#ee0000";
        }

        String[] msgLines = msg.split("\n");
        StringBuilder lines = new StringBuilder();

        for (int i = 1; i < msgLines.length; i++) {
            lines.append("\t\t\t\t\t\t\t\t\t- ").append(msgLines[i]).append("<br>");
        }

        final Spanned text = Html.fromHtml("<font color='#555555'>"
                + new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())
                + "</font> "
                + "<font color='" + fontColor + "'>"
                + msgLines[0].replace("\n", "<br>") + "</font><br>"
                + lines);

        MainActivity.addGuiLog(text);
    }

    public static void addLog(String msg) {
        addGuiLog(msg, LMT_TEXT);
    }

    public static void addLog(int resId) {
        addGuiLog(MainApp.getStr(resId), LMT_TEXT);
    }

    public static void addLog(String msg, int msgType) {
        addGuiLog(msg, msgType);
    }

    public static void addLog(Exception e) {
        addGuiLog(e.getMessage(), LMT_ERROR);
    }
}