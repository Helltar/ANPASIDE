package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.content.SharedPreferences;

public class EditorConfig {

    private Context context;

    private final String PREF_NAME_LAST_FILENAME = "lastfilename";

    public EditorConfig(Context context) {
        this.context = context;    
    }

    private SharedPreferences getSpMain() {
        return context.getSharedPreferences("editor_config", context.MODE_PRIVATE);
    }

    public String getLastFilename() {
        return getSpMain().getString(PREF_NAME_LAST_FILENAME, "");
    }

    public void setLastFilename(String filename) {
        getSpMain().edit().putString(PREF_NAME_LAST_FILENAME, filename).apply();
    }
}

