package com.github.helltar.anpaside;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private EditText edtFontSize;
    private CheckBox cbHighlighter;
    private CheckBox cbWordwrap;
    //private EditText edtGlobLibsPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edtFontSize = findViewById(R.id.edtEditorFontSize);
        cbHighlighter = findViewById(R.id.cbHighlighter);
        cbWordwrap = findViewById(R.id.cbWordwrap);
        //edtGlobLibsPath = findViewById(R.id.edtGlobalDirPath);

        edtFontSize.setText(String.valueOf(MainActivity.getInstance().editor.editorConfig.getFontSize()));
        cbHighlighter.setChecked(MainActivity.getInstance().editor.editorConfig.getHighlighterEnabled());
        cbWordwrap.setChecked(!MainActivity.getInstance().editor.editorConfig.getWordwrapEnabled());
        //edtGlobLibsPath.setText(MainActivity.ideConfig.getGlobalDirPath());
    }

    public void onBtnSaveClick(View v) {
        MainActivity.getInstance().editor.setFontSize(Integer.parseInt(edtFontSize.getText().toString()));
        MainActivity.getInstance().editor.setHighlighterEnabled(cbHighlighter.isChecked());
        MainActivity.getInstance().editor.setWordwrap(!cbWordwrap.isChecked());
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
