package com.github.helltar.anpaside;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class SettingsActivity extends Activity {

    private EditText edtFontSize;
    private CheckBox cbHighlighter;
    //private EditText edtGlobLibsPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edtFontSize = findViewById(R.id.edtEditorFontSize);
        cbHighlighter = findViewById(R.id.cbHighlighter);
        //edtGlobLibsPath = findViewById(R.id.edtGlobalDirPath);

        edtFontSize.setText(String.valueOf(MainActivity.getInstance().editor.editorConfig.getFontSize()));
        cbHighlighter.setChecked(MainActivity.getInstance().editor.editorConfig.getHighlighterEnabled());
        //edtGlobLibsPath.setText(MainActivity.ideConfig.getGlobalDirPath());
    }

    public void onBtnSaveClick(View v) {
       MainActivity.getInstance().editor.setFontSize(Integer.parseInt(edtFontSize.getText().toString()));
        MainActivity.getInstance().editor.setHighlighterEnabled(cbHighlighter.isChecked());
        /*
        String path = edtGlobLibsPath.getText().toString();

        if (!path.endsWith("/")) {
            path += "/";
        }

        MainActivity.ideConfig.setGlobalDirPath(path);
        */
        finish();
    }
}
