package com.github.helltar.anpaside;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class SettingsActivity extends Activity {

    private EditText edtFontSize;
    private CheckBox cbHighlighter;
    private EditText edtGlobLibsPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edtFontSize = findViewById(R.id.edtEditorFontSize);
        cbHighlighter = findViewById(R.id.cbHighlighter);
        edtGlobLibsPath = findViewById(R.id.edtGlobalDirPath);

        edtFontSize.setText("" + MainActivity.editor.editorConfig.getFontSize());
        cbHighlighter.setChecked(MainActivity.editor.editorConfig.getHighlighterEnabled());
        edtGlobLibsPath.setText(MainActivity.ideConfig.getGlobalDirPath());
    }

    public void onBtnSaveClick(View v) {
        MainActivity.editor.setFontSize(Integer.parseInt(edtFontSize.getText().toString()));
        MainActivity.editor.setHighlighterEnabled(cbHighlighter.isChecked());

        String path = edtGlobLibsPath.getText().toString();

        if (!path.endsWith("/")) {
            path += "/";
        }

        MainActivity.ideConfig.setGlobalDirPath(path);

        finish();
    }
}
