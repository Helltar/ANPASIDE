package com.github.helltar.anpaside;

import com.github.helltar.anpaside.MainApp;

public class Consts {

    public static final String DATA_PKG_PATH = MainApp.getContext().getApplicationInfo().dataDir + "/";

    public static final String MP3CC = "mp3cc";
    public static final String FW_CLASS = "FW.class";

    public static final String ASSET_DIR_BIN = "bin";
    public static final String ASSET_DIR_FILES = "files";
    public static final String ASSET_DIR_STUBS = "stubs";

    public static final String DIR_MAIN = "AppProjects/";
    public static final String DIR_BIN = "bin/";
    public static final String DIR_SRC = "src/";
    public static final String DIR_LIBS = "libs/";
    public static final String DIR_RES = "res/";
    public static final String DIR_PREBUILD = "prebuild/";

    public static final String EXT_PROJ = ".aproj";
    public static final String EXT_PAS = ".pas";
    public static final String EXT_JAR = ".jar";

    public static final String TPL_HELLOWORLD = getString(R.string.tpl_helloworld);
    public static final String TPL_MODULE = getString(R.string.tpl_module);
    public static final String TPL_GITIGNORE = getString(R.string.tpl_gitignore);
    public static final String TPL_MANIFEST = getString(R.string.tpl_manifest);

    private static String getString(int resId) {
        return MainApp.getContext().getString(resId);
    }
}

