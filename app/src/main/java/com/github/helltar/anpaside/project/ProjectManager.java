package com.github.helltar.anpaside.project;

import static com.github.helltar.anpaside.Consts.ASSET_DIR_FILES;
import static com.github.helltar.anpaside.Consts.DATA_PKG_PATH;
import static com.github.helltar.anpaside.Consts.DIR_BIN;
import static com.github.helltar.anpaside.Consts.DIR_LIBS;
import static com.github.helltar.anpaside.Consts.DIR_PREBUILD;
import static com.github.helltar.anpaside.Consts.DIR_RES;
import static com.github.helltar.anpaside.Consts.DIR_SRC;
import static com.github.helltar.anpaside.Consts.EXT_PAS;
import static com.github.helltar.anpaside.Consts.EXT_PROJ;
import static com.github.helltar.anpaside.Consts.TPL_GITIGNORE;
import static com.github.helltar.anpaside.Consts.TPL_HELLOWORLD;
import static com.github.helltar.anpaside.Consts.TPL_MODULE;
import static com.github.helltar.anpaside.Utils.copyFileToDir;
import static com.github.helltar.anpaside.Utils.createTextFile;
import static com.github.helltar.anpaside.Utils.getFileNameOnly;
import static com.github.helltar.anpaside.Utils.mkdir;

import com.github.helltar.anpaside.logging.Logger;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;

public class ProjectManager extends ProjectConfig {

    private String projectPath = "";
    private String projectConfigFilename = "";
    private String mainModuleFilename = "";
    private String projLibsDir = "";

    private boolean createConfigFile(String filename, String midletName) {
        setMidletName(midletName);
        setMainModuleName(midletName.toLowerCase());
        setMidletVendor("vendor");
        setVersion("1.0");

        try {
            save(filename);
            return true;
        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public boolean createProject(String path, String name) {
        projectPath = path + name + "/";
        projectConfigFilename = projectPath + name + EXT_PROJ;

        if (mkProjectDirs(projectPath)
            && createConfigFile(projectConfigFilename, name)
            && createHW(projectPath + DIR_SRC + name.toLowerCase() + EXT_PAS)) {
            createGitIgnore(projectPath);
            copyFileToDir(DATA_PKG_PATH + ASSET_DIR_FILES + "/icon.png", projectPath + DIR_RES);
            return true;
        }

        return false;
    }

    public boolean openProject(String filename) {
        try {
            open(filename);

            projectPath = FilenameUtils.getFullPath(filename);
            projectConfigFilename = filename;
            mainModuleFilename = projectPath + DIR_SRC + getMainModuleName() + EXT_PAS;
            projLibsDir = projectPath + DIR_LIBS;

            return true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public boolean mkProjectDirs(String path) {
        return mkdir(path + DIR_BIN)
                && mkdir(path + DIR_SRC)
                && mkdir(path + DIR_PREBUILD)
                && mkdir(path + DIR_RES)
                && mkdir(path + DIR_LIBS);
    }

    public boolean isProjectOpen() {
        return !projectPath.isEmpty();
    }

    public String getProjectConfigFilename() {
        return projectConfigFilename;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getProjLibsDir() {
        return projLibsDir;
    }

    public String getMainModuleFilename() {
        return mainModuleFilename;
    }

    public String getMidletVersion() {
        return getVersion();
    }

    private void createGitIgnore(String path) {
        createTextFile(path + ".gitignore", TPL_GITIGNORE);
    }

    private boolean createHW(String filename) {
        return createTextFile(filename, String.format(TPL_HELLOWORLD, getFileNameOnly(filename)));
    }

    public boolean createModule(String filename) {
        return createTextFile(filename, String.format(TPL_MODULE, getFileNameOnly(filename)));
    }
}

