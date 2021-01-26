package com.github.helltar.anpaside.ide;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import static com.github.helltar.anpaside.Consts.*;

public class IdeConfig {

    private final String PREF_NAME_INSTALL = "install";
    private final String PREF_NAME_GLOBAL_DIR_PATH = "global_libs_dir";

    private Context context;

    public IdeConfig(Context context) {
        this.context = context;
    }

    private SharedPreferences getSpMain() {
        return context.getSharedPreferences("ide_config", context.MODE_PRIVATE);
    }

    private SharedPreferences.Editor getMainEditor() {
        return getSpMain().edit();   
    }

    private boolean getInstState() {
        return getSpMain().getBoolean(PREF_NAME_INSTALL, false);
    }

    public void setInstState(boolean val) {
        getMainEditor().putBoolean(PREF_NAME_INSTALL, val).apply();
    }

    public String getGlobalDirPath() {
        return getSpMain().getString(PREF_NAME_GLOBAL_DIR_PATH, Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DIR_MAIN + "/" + DIR_LIBS);
    }

    public void setGlobalDirPath(String path) {
        getMainEditor().putString(PREF_NAME_GLOBAL_DIR_PATH, path).apply();
    }

    public boolean isAssetsInstall() {
        return getInstState();
    }
}
