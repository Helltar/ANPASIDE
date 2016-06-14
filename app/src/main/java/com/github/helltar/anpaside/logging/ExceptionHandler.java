/*
 * Copyright 2011 Oleg Elifantiev
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.helltar.anpaside.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

final class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final DateFormat formatter = new SimpleDateFormat("dd.MM.yy HH:mm");
    private final DateFormat fileFormatter = new SimpleDateFormat("dd-MM-yy");
    private String versionName = "0";
    private int versionCode = 0;
    private final String stacktraceDir;
    private final Thread.UncaughtExceptionHandler previousHandler;

    private ExceptionHandler(Context context, boolean chained) {
        PackageManager mPackManager = context.getPackageManager();
        PackageInfo mPackInfo;

        try {
            mPackInfo = mPackManager.getPackageInfo(context.getPackageName(), 0);
            versionName = mPackInfo.versionName;
            versionCode = mPackInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }

        if (chained) {
            previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        } else {
            previousHandler = null;
        }

        stacktraceDir = String.format("/Android/data/%s/logs/", context.getPackageName());
    }

    static ExceptionHandler inContext(Context context) {
        return new ExceptionHandler(context, true);
    }

    static ExceptionHandler reportOnlyHandler(Context context) {
        return new ExceptionHandler(context, false);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        final String state = Environment.getExternalStorageState();
        final Date dumpDate = new Date(System.currentTimeMillis());

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            StringBuilder reportBuilder = new StringBuilder();

            reportBuilder
                .append("\n\n######################################################\n\n")
                .append(formatter.format(dumpDate)).append("\n")
                .append(String.format("Version: %s (%d)\n", versionName, versionCode))
                .append(thread.toString()).append("\n");

            processThrowable(exception, reportBuilder);

            File sd = Environment.getExternalStorageDirectory();
            File stacktrace = new File(sd.getPath() + stacktraceDir,
                                       String.format("stacktrace-%s.txt", 
                                                     fileFormatter.format(dumpDate)));
            File dumpdir = stacktrace.getParentFile();
            boolean dirReady = dumpdir.isDirectory() || dumpdir.mkdirs();

            if (dirReady) {
                FileWriter writer = null;

                try {
                    writer = new FileWriter(stacktrace, true);
                    writer.write(reportBuilder.toString());
                } catch (IOException e) {
                    // ignore
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

        if (previousHandler != null) {
            previousHandler.uncaughtException(thread, exception);
        }
    }

    private void processThrowable(Throwable exception, StringBuilder builder) {
        if (exception == null) {
            return;
        }

        StackTraceElement[] stackTraceElements = exception.getStackTrace();

        builder
            .append("Exception: ").append(exception.getClass().getName()).append("\n")
            .append("Message: ").append(exception.getMessage()).append("\nStacktrace:\n");

        for (StackTraceElement element : stackTraceElements) {
            builder.append("\t").append(element.toString()).append("\n");
        }

        processThrowable(exception.getCause(), builder);
    }
}

