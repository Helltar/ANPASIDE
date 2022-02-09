package com.github.helltar.anpaside.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.github.helltar.anpaside.R;
import com.github.helltar.anpaside.editor.EditorConfig;

public class SettingsActivity extends AppCompatActivity {

    private final EditorConfig editorConfig = new EditorConfig(this);

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

        edtFontSize.setText(String.valueOf(editorConfig.getFontSize()));
        cbHighlighter.setChecked(editorConfig.getHighlighterEnabled());
        cbWordwrap.setChecked(!editorConfig.getWordwrapEnabled());
        //edtGlobLibsPath.setText(MainActivity.ideConfig.getGlobalDirPath());
    }

    public void onBtnSaveClick(View v) {
        Intent data = new Intent();

        data.putExtra(editorConfig.FONT_SIZE, Integer.parseInt(edtFontSize.getText().toString()));
        data.putExtra(editorConfig.HIGHLIGHTER_ENABLED, cbHighlighter.isChecked());
        data.putExtra(editorConfig.WORDWRAP, !cbWordwrap.isChecked());

        setResult(1, data);

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
