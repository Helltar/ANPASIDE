package com.github.helltar.anpaside.project;

import com.github.helltar.anpaside.ProcessResult;
import com.github.helltar.anpaside.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.io.FileUtils;

import static com.github.helltar.anpaside.logging.Logger.*;
import static com.github.helltar.anpaside.Utils.*;
import static com.github.helltar.anpaside.Consts.*;

public class ProjectBuilder extends ProjectManager {

    private final String mp3cc;
    private final String stubsDir;
    private final String globLibsDir;

    private final String projPrebuildDir;

    public ProjectBuilder(String filename, String mp3cc, String stubsDir, String globLibsDir) {
        this.mp3cc = mp3cc;
        this.stubsDir = stubsDir;
        this.globLibsDir = globLibsDir;

        openProject(filename);

        projPrebuildDir = getProjectPath() + DIR_PREBUILD;
    }

    private ProcessResult runCompiler(String filename, boolean detectUnits) {
        String args =
            mp3cc
            + " -s " + filename
            + " -o " + projPrebuildDir
            + " -l " + globLibsDir
            + " -p " + getProjLibsDir()
            + " -m " + Integer.toString(getMathType())
            + " c " + Integer.toString(getCanvasType());

        if (detectUnits) {
            args += " -d";
        }

        return runProc(args);
    }

    public boolean compile(final String filename) {
        ProcessResult compilerProc = runCompiler(filename, true);

        if (!compilerProc.started) {
            return false;
        }

        String output = compilerProc.output;

        Matcher m = Pattern.compile("\\^0(.*?)\n").matcher(output);

        while (m.find()) {
            String unitName = m.group(1);
            String unitFilename = getProjectPath() + DIR_SRC + unitName + EXT_PAS;

            if (fileExists(unitFilename, true)
                && !fileExists(projPrebuildDir + unitName + EXT_CLASS)
                && compile(unitFilename)) {
                continue;
            } else {
                return false;
            }
        }

        // компиляция родителя
        compilerProc = runCompiler(filename, false);

        if (!compilerProc.started) {
            return false;
        }

        output = compilerProc.output;

        // очистка ненужной информации
        String cleanOutput = deleteCharacters(output);

        if (!isErr(output)) {
            Logger.addLog(cleanOutput);

            if (findAndCopyStubs(output) && findAndCopyLib(output)) {
                return true;
            }
        } else {
            Logger.addLog(cleanOutput, LMT_ERROR);
        }

        return false;
    }

    private boolean isErr(String output) {
        return output.contains("[Pascal Error]") || output.contains("Fatal error");
    }

    private boolean findAndCopyLib(String output) {
        Matcher m = Pattern.compile("\\^1(.*?)\n").matcher(output);

        while (m.find()) {
            String libName = "Lib_" + m.group(1) + EXT_CLASS;

            // пробуем найти библиотеку в libs каталоге проекта
            if (!copyFileToDir(getProjLibsDir() + libName, projPrebuildDir, false)) {
                // если нет берем из глобального
                if (!copyFileToDir(globLibsDir + libName, projPrebuildDir)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean findAndCopyStubs(String output) {
        Matcher m = Pattern.compile("\\^2(.*?)\n").matcher(output);

        while (m.find()) {
            if (!copyFileToDir(stubsDir + m.group(1), projPrebuildDir)) {
                return false;
            }
        }

        return true;
    }

    public boolean build() {
        String jarFilename = getJarFilename();

        if (prebulid()
            && compile(getMainModuleFilename())
            && createZip(projPrebuildDir, jarFilename)
            && addResToZip(getProjectPath() + DIR_RES, jarFilename)) {
            Logger.addLog(LANG_MSG_BUILD_SUCCESSFULLY + "\n" 
                          + DIR_BIN + getMidletName() + EXT_JAR + "\n"
                          + getFileSize(jarFilename) + " KB", LMT_INFO);

            return true;
        }

        return false;
    }

    private boolean prebulid() {
        if (!mkProjectDirs(getProjectPath())) {
            return false;
        }

        try {
            FileUtils.cleanDirectory(new File(projPrebuildDir));
        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        String manifestDir = projPrebuildDir + "META-INF";

        if (mkdir(manifestDir)
            && createManifest(manifestDir)
            && copyFileToDir(stubsDir + "/" + FW_CLASS, projPrebuildDir)) {
            return true;
        }

        return false;
    }

    private boolean isDirEmpty(String dirPath) {
        File file = new File(dirPath);
        return file.isDirectory() && file.list().length <= 0;
    }

    public String getJarFilename() {
        return getProjectPath() + DIR_BIN + getMidletName() + EXT_JAR;
    }

    private String deleteCharacters(String output) {
        String[] lines = output.split("\n");
        StringBuilder cleanOutput = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].startsWith("@")) {
                cleanOutput.append(lines[i] + "\n");
            }
        }

        return cleanOutput.toString()
            .replace("[Pascal Error]", "")
            .replace("^1", "Lib: ")
            .replace("^2", "")
            .replace("^3", "")
            .trim();
    }

    private boolean createManifest(String path) {
        int midp = getCanvasType() < 1 ? 1 : 2;
        int cldc = midp == 2 ? 1 : 0;

        return createTextFile(path + "/MANIFEST.MF",
                              String.format(TPL_MANIFEST,
                                            getMidletName(), getMidletVendor(),
                                            getMidletName(), getMidletVersion(),
                                            cldc, midp));
    }

    private boolean addResToZip(String resDir, String zipFilename) {
        if (isDirEmpty(resDir)) {
            return true;
        }

        return createZip(resDir, zipFilename, true);
    }

    private boolean createZip(String dirPath, String zipFilename) {
        return createZip(dirPath, zipFilename, false);
    }

    private boolean createZip(String dirPath, String zipFilename, boolean isAddToArchive) {
        ZipParameters param = new ZipParameters();

        param.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        param.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_ULTRA); 
        param.setIncludeRootFolder(false);

        if (isAddToArchive) {
            param.setRootFolderInZip("/");
        }

        try {
            new ZipFile(zipFilename).addFolder(dirPath, param);
            return true;
        } catch (ZipException ze) {
            Logger.addLog(
                LANG_ERR_FAILED_CREATE_ARCHIVE + ": " + dirPath + " (" + ze.getMessage() + ")",
                LMT_ERROR);
        }

        return false;
    }
}

