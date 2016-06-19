package com.github.helltar.anpaside.project;

import com.github.helltar.anpaside.logging.Logger;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;

import static com.github.helltar.anpaside.Consts.*;
import static com.github.helltar.anpaside.Utils.*;

public class ProjectManager {

    private static ProjectConfig projConfig;

    private static String currentProjectPath = "";
    private static String mainModuleFilename = "";

    private static boolean createConfigFile(String filename) {
        final String projName = getFileNameOnly(filename);

        try {
            new ProjectConfig(filename) {{
                    setMainModuleName(projName.toLowerCase());
                    setMathType(0);
                    setCanvasType(1);

                    setMidletName(projName);
                    setMidletVendor("vendor");
                    setMidletIcon("/icon.png");

                    setVersMajor(1);
                    setVersMinor(0);
                    setVersBuild(0);

                    save();
                }};

            return true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public static boolean createProject(String path, String name) {
        String projPath = path + name + "/";

        if (mkProjectDirs(projPath)
            && createConfigFile(projPath + name + EXT_PROJ)
            && createHW(projPath + DIR_SRC + name.toLowerCase() + EXT_PAS)) {
            createGitIgnore(projPath);
            copyFileToDir(DATA_PKG_PATH + ASSET_DIR_FILES + "/icon.png", projPath + DIR_RES);
            return true;
        }

        return false;
    }

    public static boolean openProject(String filename) {
        try {
            projConfig = new ProjectConfig(filename);

            String projPath = FilenameUtils.getFullPath(filename);

            currentProjectPath = projPath;
            mainModuleFilename = projPath + DIR_SRC + projConfig.getMainModuleName() + EXT_PAS;

            return true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public static boolean mkProjectDirs(String path) {
        if (mkdir(path + DIR_BIN)
            && mkdir(path + DIR_SRC)
            && mkdir(path + DIR_PREBUILD)
            && mkdir(path + DIR_RES)
            && mkdir(path + DIR_LIBS)) {
            return true;
        }

        return false;
    }

    public static boolean isProjectOpen() {
        return !currentProjectPath.isEmpty();
    }

    public static String getCurrentProjectPath() {
        return currentProjectPath;
    }

    public static String getMainModuleFilename() {
        return mainModuleFilename;
    }

    private static boolean createGitIgnore(String path) {
        return createTextFile(path + ".gitignore", TPL_GITIGNORE);
    }

    private static boolean createHW(String filename) {
        return createTextFile(filename, String.format(TPL_HELLOWORLD, getFileNameOnly(filename)));
    }

    public static boolean createModule(String filename) {
        return createTextFile(filename, String.format(TPL_MODULE, getFileNameOnly(filename)));
    }
}

