package com.github.helltar.anpaside.editor;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import com.github.helltar.anpaside.Logger;
import com.github.helltar.anpaside.MainApp;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

public class CodeEditor {

    private TabHost tabHost;
    private TabSpec tabSpec;

    private Map<String, Boolean> fileModifiedStatusMap = new HashMap<>();

    public CodeEditor(TabHost tabHost) {
        this.tabHost = tabHost;
    }

    public boolean openFile(String filename) {
        String text = "";

        try {
            text = FileUtils.readFileToString(new File(filename));
        } catch (IOException ioe) {
            Logger.addLog(ioe);
            return false;
        }

        final EditText editText = new EditText(MainApp.getContext());

        editText.setTextColor(Color.rgb(220, 220, 220));
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setTextSize(14);
        editText.setGravity(Gravity.TOP);

        editText.setText(text);

        editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) { }

                @Override
                public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
                    setFileModifiedStatus(getCurrentFilename(), true);
                }

                @Override
                public void afterTextChanged(Editable p1) { }
            });

        tabSpec = tabHost.newTabSpec(filename);
        tabSpec.setIndicator(new File(filename).getName());
        tabSpec.setContent(new TabContentFactory() {
                @Override
                public View createTabContent(String p1) {
                    return editText;
                }
            });

        tabHost.addTab(tabSpec);
        tabHost.setCurrentTabByTag(filename);

        setFileModifiedStatus(filename, false);

        return true;
    }

    private boolean saveFile(String filename) {
        try {
            FileUtils.writeStringToFile(new File(getCurrentFilename()),
                                        getCurrentEditor().getText().toString());

            setFileModifiedStatus(getCurrentFilename(), false);

            return true;

        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public boolean saveCurrentFile() {
        return saveFile(getCurrentFilename());
    }

    private EditText getCurrentEditor() {
        // TODO: !
        return (EditText) tabHost.getCurrentView();
    }

    private String getCurrentFilename() {
        return tabHost.getCurrentTabTag();
    }

    private void setFileModifiedStatus(String filename, boolean status) {
        fileModifiedStatusMap.put(filename, status);
    }

    public boolean isCurrentFileModified() {
        if (!fileModifiedStatusMap.containsKey(getCurrentFilename())) {
            return false;
        }

        return fileModifiedStatusMap.get(getCurrentFilename());
    }
}

