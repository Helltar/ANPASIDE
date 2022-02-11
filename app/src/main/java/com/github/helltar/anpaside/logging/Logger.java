package com.github.helltar.anpaside.logging;

import static com.github.helltar.anpaside.Consts.COLOR_LOGGER_DATE;
import static com.github.helltar.anpaside.Consts.COLOR_LOGGER_ERROR;
import static com.github.helltar.anpaside.Consts.COLOR_LOGGER_FONT;
import static com.github.helltar.anpaside.Consts.COLOR_LOGGER_INFO;

import android.text.Html;
import android.text.Spanned;

import com.github.helltar.anpaside.activities.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Logger {

    private static final int LMT_TEXT = 0;
    public static final int LMT_INFO = 1;
    public static final int LMT_ERROR = 2;

    private static void addLogToGUI(String msg, int msgType) {
        if (msg.isEmpty()) {
            return;
        }

        String fontColor = COLOR_LOGGER_FONT;

        if (msgType == LMT_INFO) {
            fontColor = COLOR_LOGGER_INFO;
        } else if (msgType == LMT_ERROR) {
            fontColor = COLOR_LOGGER_ERROR;
        }

        String[] msgLines = msg.split("\n");
        StringBuilder lines = new StringBuilder();

        for (int i = 1; i < msgLines.length; i++) {
            lines.append("\t\t\t\t\t\t\t\t\t- ").append(msgLines[i]).append("<br>");
        }

        final Spanned text = Html.fromHtml("<font color='" + COLOR_LOGGER_DATE + "'>"
                + new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date())
                + "</font> "
                + "<font color='" + fontColor + "'>"
                + msgLines[0].replace("\n", "<br>") + "</font><br>"
                + lines);

        MainActivity.addLogToGUI(text);
    }

    public static void addLog(String msg) {
        addLogToGUI(msg, LMT_TEXT);
    }

    public static void addLog(String msg, int msgType) {
        addLogToGUI(msg, msgType);
    }

    public static void addLog(Exception e) {
        addLogToGUI(Objects.requireNonNull(e.getMessage(), "null"), LMT_ERROR);
    }
}