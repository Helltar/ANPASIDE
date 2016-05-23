package com.github.helltar.anpaside.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import com.github.helltar.anpaside.Logger;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

public class CodeEditor {

    private EditText edtText;
    private String currentFilename = "";
    private boolean currentFileModified;

    public CodeEditor(EditText editor) {
        edtText = editor;

        edtText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence p1, int p2, int p3, int p4) { }

                @Override
                public void onTextChanged(CharSequence p1, int p2, int p3, int p4) {
                    currentFileModified = true;
                }

                @Override
                public void afterTextChanged(Editable p1) { }
            });
    }

    public boolean openFile(String filename) {
        try {
            edtText.setText(FileUtils.readFileToString(new File(filename)));
            currentFilename = filename;
            return true;
        } catch (IOException ioe) {
            Logger.addLog(ioe);
        }

        return false;
    }

    private boolean saveFile(String filename) {
        try {        
            FileUtils.writeStringToFile(new File(filename), edtText.getText().toString());
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

    public boolean isCurrentFileModified() {
        return currentFileModified;
    }

    public void setEnabled(boolean enabled) {
        edtText.setEnabled(enabled);
    }
}

