package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.content.SharedPreferences;

public class EditorConfig {

    private final Context context;

    private final String RECENT_FILENAMES = "recent_filenames";
    private final String LAST_PROJECT = "last_project";
    public final String FONT_SIZE = "font_size";
    public final String HIGHLIGHTER_ENABLED = "highlighter_enabled";
    public final String WORDWRAP = "wordwrap";

    public EditorConfig(Context context) {
        this.context = context;
    }

    private SharedPreferences getSpMain() {
        return context.getSharedPreferences("editor_config", Context.MODE_PRIVATE);
    }

    public String getRecentFilenames() {
        return getSpMain().getString(RECENT_FILENAMES, "");
    }

    public void setRecentFilenames(String filenames) {
        getSpMain().edit().putString(RECENT_FILENAMES, filenames).apply();
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

    public boolean getWordwrapEnabled() {
        return getSpMain().getBoolean(WORDWRAP, true);
    }

    public void setWordwrapEnabled(boolean we) {
        getSpMain().edit().putBoolean(WORDWRAP, we).apply();
    }
}
