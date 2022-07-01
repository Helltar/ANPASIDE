package com.github.helltar.anpaside.activities;

import static com.github.helltar.anpaside.Consts.RCODE_SETTINGS;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edtFontSize = findViewById(R.id.edtEditorFontSize);
        cbHighlighter = findViewById(R.id.cbHighlighter);
        cbWordwrap = findViewById(R.id.cbWordwrap);

        edtFontSize.setText(String.valueOf(editorConfig.getFontSize()));
        cbHighlighter.setChecked(editorConfig.getHighlighterEnabled());
        cbWordwrap.setChecked(!editorConfig.getWordwrapEnabled());
    }

    public void onBtnSaveClick(View v) {
        Intent data = new Intent();

        int fontSize = Integer.parseInt(edtFontSize.getText().toString());

        if (fontSize < 4) {
            fontSize = 4;
        } else if (fontSize > 64) {
            fontSize = 64;
        }

        data.putExtra(editorConfig.FONT_SIZE, fontSize);
        data.putExtra(editorConfig.HIGHLIGHTER_ENABLED, cbHighlighter.isChecked());
        data.putExtra(editorConfig.WORDWRAP, !cbWordwrap.isChecked());

        setResult(RCODE_SETTINGS, data);

        finish();
    }
}