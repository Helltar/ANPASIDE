package com.github.helltar.anpaside;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import com.github.helltar.anpaside.logging.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import static com.github.helltar.anpaside.Consts.*;
import static com.github.helltar.anpaside.logging.Logger.*;

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
            FileUtils.writeStringToFile(new File(filename), text);
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
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;

                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
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

        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
