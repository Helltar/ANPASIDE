package com.github.helltar.anpaside;

import com.github.helltar.anpaside.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Utils {

    public static boolean mkdir(String dirName) {
        if (new File(dirName).mkdirs()) {
            return true;
        } else {
            Logger.addLog("Не удалось создать каталог: " + dirName);
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
    public static String runProc(String[] args) {
        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            Process process = processBuilder.start();

            InputStream inputStream = process.getInputStream();
            int c;

            while ((c = inputStream.read()) > 0) {
                output.append((char) c);
            }

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return output.toString();
    }
}

