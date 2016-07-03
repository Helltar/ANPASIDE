package com.github.helltar.anpaside;

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
        if (fileExists(srcFile, true)) {
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
        if (new File(filename).exists()) {
            return true;
        } else if (showErrMsg) {
            Logger.addLog(LANG_ERR_FILE_NOT_FOUND + ": " + filename, LMT_ERROR);
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

    // TODO: bool
    public static String runProc(String args) {
        StringBuffer output = new StringBuffer();

        try {
            Process process = Runtime.getRuntime().exec(args);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = "";

            while ((line = reader.readLine()) != null) {
                output.append(line + "\n");
                process.waitFor();
            }

        } catch (InterruptedException ie) {
            Logger.addLog(ie);
        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return output.toString();
    }
}

