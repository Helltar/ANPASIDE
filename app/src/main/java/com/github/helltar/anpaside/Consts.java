package com.github.helltar.anpaside;

import android.os.Environment;

public class Consts {

    public static final String DATA_PKG_PATH = MainApp.getContext().getApplicationInfo().dataDir + "/";
    public static final String DATA_LIB_PATH = MainApp.getContext().getApplicationInfo().nativeLibraryDir + "/";

    public static final String MP3CC = "libmp3cc.so";
    public static final String FW_CLASS = "FW.class";

    public static final String ASSET_DIR_FILES = "files";
    public static final String ASSET_DIR_STUBS = "stubs";

    public static final String DIR_PROJECTS = "projects/";
    public static final String DIR_BIN = "bin/";
    public static final String DIR_SRC = "src/";
    public static final String DIR_LIBS = "libs/";
    public static final String DIR_RES = "res/";
    public static final String DIR_PREBUILD = "prebuild/";

    public static final String WORK_DIR_PATH =
            Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS) + "/" +
                    getString(R.string.app_name) + "/";

    public static final String EXT_PROJ = ".aproj";
    public static final String EXT_PAS = ".pas";
    public static final String EXT_JAR = ".jar";
    public static final String EXT_CLASS = ".class";

    public static final String TPL_HELLOWORLD = getString(R.string.tpl_helloworld);
    public static final String TPL_MODULE = getString(R.string.tpl_module);
    public static final String TPL_GITIGNORE = getString(R.string.tpl_gitignore);
    public static final String TPL_MANIFEST = getString(R.string.tpl_manifest);

    // strings
    public static final String LANG_MSG_BUILD_SUCCESSFULLY = getString(R.string.msg_build_successfully);
    public static final String LANG_ERR_FAILED_CREATE_ARCHIVE = getString(R.string.err_failed_create_archive);
    public static final String LANG_ERR_FILE_NOT_FOUND = getString(R.string.err_file_not_found);
    public static final String LANG_ERR_CREATE_DIR = getString(R.string.err_create_dir);

    private static String getString(int resId) {
        return MainApp.getStr(resId);
    }
}