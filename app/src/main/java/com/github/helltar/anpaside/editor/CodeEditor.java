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
import android.widget.Toast;
import com.github.helltar.anpaside.MainActivity;
import com.github.helltar.anpaside.R;
import com.github.helltar.anpaside.logging.Logger;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class CodeEditor {

    private Context context;
    private TabHost tabHost;
    public EditorConfig editorConfig;

    private String btnTabCloseName = "Close";
    private String tabIns = "    ";
    private int fontColor = Color.rgb(220, 220, 220);
    private Typeface fontTypeface = Typeface.MONOSPACE;

    private Map<String, Boolean> fileModifiedStatusMap = new HashMap<>();
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

        edtText.setTextSize(editorConfig.getFontSize());
        edtText.setTextColor(fontColor);
        edtText.setTypeface(fontTypeface);

        edtText.setBackgroundColor(android.R.color.transparent);
        edtText.setGravity(Gravity.TOP);
        edtText.setHorizontallyScrolling(true);
        edtText.addTextChangedListener(textWatcher);
        edtText.setOnKeyListener(keyListener);

        edtText.setText(text);

        filenameList.add(filename);
        setFileModifiedStatus(filename, false);
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
            setFileModifiedStatus(getCurrentFilename(), true);
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
                        editText.getText().insert(editText.getSelectionStart(),
                                                  tabIns);
                        return true;

                    case KeyEvent.KEYCODE_S:
                        if (keyEvent.isCtrlPressed()) {
                            saveCurrentFile();
                            showToastFileSaved();
                            return true;
                        }

                        return false;
                }
            }

            return false;
        }
    };

    private void highlights(Editable s) {
        Highlighter.clearSpans(s);

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

    public boolean saveCurrentFile() {
        if (isEditorActive()) {
            try {
                FileUtils.writeStringToFile(new File(getCurrentFilename()),
                                            getCurrentEditor().getText().toString());
                setFileModifiedStatus(getCurrentFilename(), false);
                return true;
            } catch (IOException ioe) {
                Logger.addLog(ioe);
            }
        }

        return false;
    }

    public EditText getCurrentEditor() {
        return (EditText) tabHost.getTabContentView().findViewWithTag(getCurrentFilename());
    }

    private boolean isEditorActive() {
        return getCurrentEditor() != null;
    }

    private void closeFile(String filename) {
        // TODO: try del
        tabHost.getTabWidget().getChildAt(tabHost.getCurrentTab()).setVisibility(View.GONE);
        tabHost.getTabContentView().removeView(tabHost.getCurrentView());

        fileModifiedStatusMap.remove(filename);
        filenameList.remove(filename);

        if (!filenameList.isEmpty()) {
            tabHost.setCurrentTabByTag(filenameList.getLast());
        }
    }

    private void showToastFileSaved() {
        Toast toast = Toast.makeText(context, context.getString(R.string.msg_saved), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 80);
        toast.show();
    }

    private String getCurrentFilename() {
        return tabHost.getCurrentTabTag();
    }

    private void setFileModifiedStatus(String filename, boolean modifiedStatus) {
        fileModifiedStatusMap.put(filename, modifiedStatus);
    }

    private boolean isFileOpen(String filename) {
        return filenameList.contains(filename);
    }

    public boolean isCurrentFileModified() {
        String filename = getCurrentFilename();

        if (isFileOpen(filename)) {
            return fileModifiedStatusMap.get(filename);
        }

        return false;
    }

    public void setBtnTabCloseName(String name) {
        btnTabCloseName = name;
    }

    public void setHighlighterEnabled(boolean he) {
        editorConfig.setHighlighterEnabled(he);

        int ss = getCurrentEditor().getSelectionStart();
        getCurrentEditor().setText(getCurrentEditor().getText());
        getCurrentEditor().setSelection(ss);
    }

    public void setFontSize(int size) {
        editorConfig.setFontSize(size);
        getCurrentEditor().setTextSize(size);
    }
}
