package com.github.helltar.anpaside.editor;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import com.github.helltar.anpaside.MainActivity;
import com.github.helltar.anpaside.R;
import com.github.helltar.anpaside.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import org.apache.commons.io.FileUtils;

public class CodeEditor {

    private Context context;
    private TabHost tabHost;
    public EditorConfig editorConfig;

    private String btnTabCloseName = "Close";
    private String tabIns = "    ";
    private int fontColor = Color.rgb(220, 220, 220);
    private Typeface fontTypeface = Typeface.MONOSPACE;

    public static boolean isFilesModified = false;
    private LinkedList<String> filenameList = new LinkedList<>();

    public CodeEditor(Context context, TabHost tabHost) {
        this.context = context;
        this.tabHost = tabHost;
        editorConfig = new EditorConfig(context);
    }

    public boolean openFile(String filename) {
        if (isFileOpen(filename)) {
            tabHost.setCurrentTabByTag(filename);
            return true;
        }

        String text = "";

        try {
            text = FileUtils.readFileToString(new File(filename));
        } catch (IOException ioe) {
            Logger.addLog(ioe);
            return false;
        }

        final EditText edtText = new CodeEditText(context);

        edtText.setTag(filename);

        edtText.setBackgroundColor(android.R.color.transparent);
        edtText.setGravity(Gravity.TOP);
        edtText.setHorizontallyScrolling(true);

        edtText.setTextSize(editorConfig.getFontSize());
        edtText.setTextColor(fontColor);
        edtText.setTypeface(fontTypeface);
        edtText.setText(text, BufferType.SPANNABLE);

        edtText.setOnKeyListener(keyListener);
        edtText.addTextChangedListener(textWatcher);

        filenameList.add(filename);
        saveRecentFilenames();

        highlights(edtText.getEditableText());

        createTabs(filename, new File(filename).getName(), new TabContentFactory() {
                @Override
                public View createTabContent(String p1) {
                    ScrollView sv = new ScrollView(context);
                    sv.setFillViewport(true);
                    sv.addView(edtText);
                    return sv;
                }
            });

        edtText.requestFocus();

        return true;
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            MainActivity.svLog.setVisibility(View.GONE);
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

    private OnKeyListener keyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
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
        }
    };

    private void highlights(Editable s) {
        if (editorConfig.getHighlighterEnabled()) {
            Highlighter.highlights(s);
        }
    }

    private void createTabs(String tag, String title, TabContentFactory tabContent) {
        final TabSpec tabSpec = tabHost.newTabSpec(tag);

        tabSpec.setIndicator(title);
        tabSpec.setContent(tabContent);

        tabHost.addTab(tabSpec);
        tabHost.setCurrentTabByTag(tag);

        tabHost.getTabWidget().getChildAt(tabHost.getTabWidget().getChildCount() - 1)
            .setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    tabHost.setCurrentTabByTag(tabSpec.getTag());
                    showPopupMenu(v, tabSpec.getTag());
                    return true;
                }
            });
    }

    private void showPopupMenu(View v, final String tag) {
        PopupMenu pm = new PopupMenu(context, v);

        pm.getMenu().add(btnTabCloseName);
        pm.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    closeFile(tag);
                    return true;
                }
            });

        pm.show();
    }

    public boolean saveAllFiles() {
        return saveAllFiles(false);
    }

    public boolean saveAllFiles(boolean showMsg) {
        if (isEditorActive()) {
            try {
                for (int i = 0; i < filenameList.size(); i++) {
                    FileUtils.writeStringToFile(new File(filenameList.get(i)),
                                                getEditorWithTag(filenameList.get(i)).getText().toString());
                }

                isFilesModified = false;

                if (showMsg) {
                    Toast toast = Toast.makeText(context, context.getString(R.string.msg_saved), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 80);
                    toast.show();
                }

                return true;
            } catch (IOException ioe) {
                Logger.addLog(ioe);
            }
        }

        return false;
    }

    private EditText getEditorWithTag(String tag) {
        return (EditText) tabHost.getTabContentView().findViewWithTag(tag);
    }

    private boolean isEditorActive() {
        return !filenameList.isEmpty();
    }

    ////////// костыль begin

    private void closeFile(String filename) {
        saveAllFiles();
        tabHost.clearAllTabs();
        filenameList.remove(filename);
        saveRecentFilenames();

        LinkedList<String> ll = new LinkedList<>();
        ll.addAll(filenameList);
        filenameList.clear();

        if (!ll.isEmpty()) {
            for (int i = 0; i < ll.size(); i++) {
                openFile(ll.get(i));
            }

            tabHost.setCurrentTabByTag(ll.getLast());
        }
    }

    ////////// end

    public void openRecentFiles() {
        String recentFilenames = editorConfig.getRecentFilenames();

        if (!recentFilenames.isEmpty()) {
            String[] recentFiles = recentFilenames.split(", ");

            for (int i = 0; i < recentFiles.length; i++) {
                openFile(recentFiles[i]);
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

    public void setBtnTabCloseName(String name) {
        btnTabCloseName = name;
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
}
