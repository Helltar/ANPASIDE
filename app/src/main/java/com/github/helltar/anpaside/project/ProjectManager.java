package com.github.helltar.anpaside.project;

import com.github.helltar.anpaside.logging.Logger;
import java.io.IOException;
import org.apache.commons.io.FilenameUtils;

import static com.github.helltar.anpaside.Consts.*;
import static com.github.helltar.anpaside.Utils.*;

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
        if (mkdir(path + DIR_BIN)
            && mkdir(path + DIR_SRC)
            && mkdir(path + DIR_PREBUILD)
            && mkdir(path + DIR_RES)
            && mkdir(path + DIR_LIBS)) {
            return true;
        }

        return false;
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

    private boolean createGitIgnore(String path) {
        return createTextFile(path + ".gitignore", TPL_GITIGNORE);
    }

    private boolean createHW(String filename) {
        return createTextFile(filename, String.format(TPL_HELLOWORLD, getFileNameOnly(filename)));
    }

    public boolean createModule(String filename) {
        return createTextFile(filename, String.format(TPL_MODULE, getFileNameOnly(filename)));
    }
}

