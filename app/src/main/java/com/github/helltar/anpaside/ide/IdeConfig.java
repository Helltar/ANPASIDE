package com.github.helltar.anpaside.ide;

import android.content.Context;
import android.content.SharedPreferences;

public class IdeConfig {

    private final String PREF_NAME_INSTALL = "install";

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

    public boolean isAssetsInstall() {
        return getInstState();
    }
}

