package com.github.helltar.anpaside.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;
import android.widget.Toast;

import com.github.helltar.anpaside.R;
import com.github.helltar.anpaside.Utils;
import com.github.helltar.anpaside.logging.Logger;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Objects;

public class CodeEditor {

    private final Context context;
    private final TabHost tabHost;
    public EditorConfig editorConfig;

    private final Typeface fontTypeface = Typeface.MONOSPACE;

    public static boolean isFilesModified = false;
    private final LinkedList<String> filenameList = new LinkedList<>();

    public CodeEditor(Context context, TabHost tabHost) {
        this.context = context;
        this.tabHost = tabHost;
        editorConfig = new EditorConfig(context);
    }

    public void openFile(String filename) {
        if (isFileOpen(filename)) {
            tabHost.setCurrentTabByTag(filename);
        } else {
            try {
                String text = FileUtils.readFileToString(new File(filename));
                createEditText(filename, text);
            } catch (IOException ioe) {
                Logger.addLog(ioe);
            }
        }
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            ((Activity) context).findViewById(R.id.svLog).setVisibility(View.GONE);
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            isFilesModified = true;
        }

        @Override
        public void afterTextChanged(Editable s) {
            highlights(s);
        }
    };

    private void createEditText(String filename, String text) {
        var edtText = new CodeEditText(context, editorConfig.getFontSize());

        edtText.setTag(filename);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            edtText.setTextColor(context.getColor(R.color.editor_font_color));
        }

        edtText.setGravity(Gravity.TOP);
        edtText.setHorizontallyScrolling(editorConfig.getWordwrapEnabled());
        edtText.setTextSize(editorConfig.getFontSize());
        edtText.setTypeface(fontTypeface);
        edtText.setText(text, TextView.BufferType.SPANNABLE);

        edtText.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        edtText.setOnKeyListener(keyListener);
        edtText.addTextChangedListener(textWatcher);

        filenameList.add(filename);
        saveRecentFilenames();

        highlights(edtText.getEditableText());

        createTabs(filename, new File(filename).getName(), p1 -> {
            ScrollView sv = new ScrollView(context);
            sv.setFillViewport(true);
            sv.addView(edtText);
            return sv;
        });

        edtText.requestFocus();
    }

    private final OnKeyListener keyListener = (v, keyCode, keyEvent) -> {
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            String tabIns = "    ";
            switch (keyCode) {
                case KeyEvent.KEYCODE_TAB:
                    EditText editText = (EditText) v;
                    editText.getText().insert(editText.getSelectionStart(), tabIns);
                    return true;

                case KeyEvent.KEYCODE_S:
                    if (keyEvent.isCtrlPressed()) {
                        saveAllFiles(true);
                        return true;
                    }

                    return false;
            }
        }

        return false;
    };

    private void highlights(Editable s) {
        if (editorConfig.getHighlighterEnabled()) {
            Highlighter.highlights(s);
        }
    }

    private void createTabs(String tag, String title, TabContentFactory tabContent) {
        final var tabSpec = tabHost.newTabSpec(tag);

        tabSpec.setIndicator(title);
        tabSpec.setContent(tabContent);

        tabHost.addTab(tabSpec);
        tabHost.setCurrentTabByTag(tag);

        tabHost.getTabWidget().getChildAt(tabHost.getTabWidget().getChildCount() - 1)
                .setOnLongClickListener(v -> {
                    tabHost.setCurrentTabByTag(tabSpec.getTag());
                    showPopupMenu(v, tabSpec.getTag());
                    return true;
                });
    }

    private void showPopupMenu(View v, final String tag) {
        var pm = new PopupMenu(context, v);
        pm.getMenu().add(R.string.pmenu_tab_close);
        pm.setOnMenuItemClickListener(item -> {
            closeFile(tag);
            return true;
        });
        pm.show();
    }

    public boolean saveAllFiles() {
        return saveAllFiles(false);
    }

    public boolean saveAllFiles(boolean showMsg) {
        if (!isEditorActive()) {
            return false;
        }

        for (int i = 0; i < filenameList.size(); i++) {
            Utils.createTextFile(filenameList.get(i),
                    Objects.requireNonNull(
                            getEditorWithTag(filenameList.get(i)).getText()).toString());
        }

        isFilesModified = false;

        if (showMsg) {
            var toast = Toast.makeText(context, context.getString(R.string.msg_saved), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.BOTTOM, 0, 80);
            toast.show();
        }

        return true;
    }

    private CodeEditText getEditorWithTag(String tag) {
        return tabHost.getTabContentView().findViewWithTag(tag);
    }

    private boolean isEditorActive() {
        return !filenameList.isEmpty();
    }

    // ._. begin

    private void closeFile(String filename) {
        saveAllFiles();
        tabHost.clearAllTabs();
        filenameList.remove(filename);
        saveRecentFilenames();

        LinkedList<String> ll = new LinkedList<>(filenameList);
        filenameList.clear();

        if (!ll.isEmpty()) {
            for (int i = 0; i < ll.size(); i++) {
                openFile(ll.get(i));
            }

            tabHost.setCurrentTabByTag(ll.getLast());
        }
    }

    // end

    public void openRecentFiles() {
        String recentFilenames = editorConfig.getRecentFilenames();

        if (!recentFilenames.isEmpty()) {
            String[] recentFiles = recentFilenames.split(", ");

            for (String recentFile : recentFiles) {
                openFile(recentFile);
            }
        }
    }

    private void saveRecentFilenames() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < filenameList.size(); i++) {
            sb.append(filenameList.get(i)).append(", ");
        }

        editorConfig.setRecentFilenames(sb.toString());
    }

    private boolean isFileOpen(String filename) {
        return filenameList.contains(filename);
    }

    public void setHighlighterEnabled(boolean he) {
        editorConfig.setHighlighterEnabled(he);

        if (!he) {
            for (int i = 0; i < filenameList.size(); i++) {
                Highlighter.clearSpans(getEditorWithTag(filenameList.get(i)).getEditableText());
            }
        }
    }

    public void setFontSize(int size) {
        editorConfig.setFontSize(size);

        for (int i = 0; i < filenameList.size(); i++) {
            getEditorWithTag(filenameList.get(i)).setTextSize(size);
        }
    }

    public void setWordwrap(boolean ww) {
        editorConfig.setWordwrapEnabled(ww);

        for (int i = 0; i < filenameList.size(); i++) {
            getEditorWithTag(filenameList.get(i)).setHorizontallyScrolling(ww);
        }
    }
}
