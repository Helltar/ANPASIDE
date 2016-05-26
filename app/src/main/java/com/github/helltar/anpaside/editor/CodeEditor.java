package com.github.helltar.anpaside.editor;

import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import com.github.helltar.anpaside.Logger;
import com.github.helltar.anpaside.MainApp;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import android.widget.FrameLayout;

public class CodeEditor {

    private TabHost tabHost;

    private String currentFilename = "";
    private boolean currentFileModified;

    public CodeEditor(TabHost tabHost) {
        this.tabHost = tabHost;
    }

    public boolean openFile(String filename) {
        StringBuilder text;

        try {
            text = new StringBuilder(FileUtils.readFileToString(new File(filename)));
        } catch (IOException ioe) {
            Logger.addLog(ioe);
            return false;
        }

        currentFilename = filename;

        final EditText edtText = new EditText(MainApp.getContext());
        edtText.setText(text);

        TabHost.TabContentFactory TabFactory = new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                return edtText;
            }
        }; 

        TabHost.TabSpec tabSpec;
        tabSpec = tabHost.newTabSpec(filename);
        tabSpec.setIndicator(new File(filename).getName());
        tabSpec.setContent(TabFactory);

        tabHost.addTab(tabSpec);
        tabHost.setCurrentTabByTag(filename);

        return true;
    }

    private boolean saveFile(String filename) {
        try {        
            FileUtils.writeStringToFile(new File(filename), getCurrentEditor().getText().toString());
            currentFileModified = false;
            return true;
        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    public boolean saveCurrentFile() {
        return saveFile(currentFilename);
    }

    private EditText getCurrentEditor() {
        FrameLayout frameLayout = tabHost.getTabContentView();

        for (int i = 0; i < frameLayout.getChildCount(); i++) {
            View childView = frameLayout.getChildAt(i);

            if (childView instanceof EditText) {
                return (EditText) childView;
            }
        }

        // TODO: !
        return null;
    }

    public boolean isCurrentFileModified() {
        return currentFileModified;
    }
}

