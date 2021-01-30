package com.github.helltar.anpaside;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import com.github.helltar.anpaside.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static com.github.helltar.anpaside.Consts.LANG_ERR_CREATE_DIR;
import static com.github.helltar.anpaside.Consts.LANG_ERR_FILE_NOT_FOUND;
import static com.github.helltar.anpaside.logging.Logger.LMT_ERROR;

public class Utils {

    public static boolean mkdir(String dirName) {
        if (new File(dirName).mkdirs() | fileExists(dirName)) {
            return true;
        } else {
            Logger.addLog(LANG_ERR_CREATE_DIR + ": " + dirName, LMT_ERROR);
        }

        return false;
    }

    public static boolean copyFileToDir(String srcFile, String destDir) {
        return copyFileToDir(srcFile, destDir, true);
    }

    public static boolean copyFileToDir(String srcFile, String destDir, boolean showErrMsg) {
        if (fileExists(srcFile, showErrMsg)) {
            try {
                FileUtils.copyFileToDirectory(new File(srcFile), new File(destDir));
                return true;
            } catch (IOException ioe) {
                Logger.addLog(ioe);
            }
        }

        return false;
    }

    public static boolean fileExists(String filename) {
        return fileExists(filename, false);
    }

    public static boolean fileExists(String filename, boolean showErrMsg) {
        if (!filename.isEmpty()) {
            if (new File(filename).exists()) {
                return true;
            } else if (showErrMsg) {
                Logger.addLog(LANG_ERR_FILE_NOT_FOUND + ": " + filename, LMT_ERROR);
            }
        }

        return false;
    }

    public static String getFileNameOnly(String filename) {
        return FilenameUtils.getBaseName(filename);
    }

    public static long getFileSize(String filename) {
        return new File(filename).length() / 1024;
    }

    public static boolean createTextFile(String filename, String text) {
        try {
            FileUtils.writeStringToFile(
                new File(filename), text, StandardCharsets.UTF_8
            );
            return true;
        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public static ProcessResult runProc(String args) {
        boolean result = false;
        StringBuffer output = new StringBuffer();

        try {
            Process process = Runtime.getRuntime().exec(args);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = "";

            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
                process.waitFor();
            }

            result = true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        } catch (InterruptedException ie) {
            Logger.addLog(ie);
        }

        return new ProcessResult(result, output.toString());
    }

    public static String getPathFromUri(final Context context, final Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return uri.toString();
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return uri.toString();
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }
}
