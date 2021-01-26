package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.content.SharedPreferences;

public class EditorConfig {

    private Context context;

    private final String LAST_FILENAME = "lastfilename";
    private final String LAST_PROJECT = "last_project";
    private final String FONT_SIZE = "font_size";
    private final String HIGHLIGHTER_ENABLED = "highlighter_enabled";

    public EditorConfig(Context context) {
        this.context = context;
    }

    private SharedPreferences getSpMain() {
        return context.getSharedPreferences("editor_config", context.MODE_PRIVATE);
    }

    public String getLastFilename() {
        return getSpMain().getString(LAST_FILENAME, "");
    }

    public void setLastFilename(String filename) {
        getSpMain().edit().putString(LAST_FILENAME, filename).apply();
    }

    public String getLastProject() {
        return getSpMain().getString(LAST_PROJECT, "");
    }

    public void setLastProject(String filename) {
        getSpMain().edit().putString(LAST_PROJECT, filename).apply();
    }

    public int getFontSize() {
        return getSpMain().getInt(FONT_SIZE, 14);
    }

    public void setFontSize(int size) {
        getSpMain().edit().putInt(FONT_SIZE, size).apply();
    }

    public boolean getHighlighterEnabled() {
        return getSpMain().getBoolean(HIGHLIGHTER_ENABLED, true);
    }

    public void setHighlighterEnabled(boolean he) {
        getSpMain().edit().putBoolean(HIGHLIGHTER_ENABLED, he).apply();
    }
}
