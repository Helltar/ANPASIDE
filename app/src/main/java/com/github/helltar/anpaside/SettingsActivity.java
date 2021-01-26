package com.github.helltar.anpaside;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class SettingsActivity extends Activity {

    private EditText edtGlobLibsPath;
    private EditText edtFontSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        edtGlobLibsPath = findViewById(R.id.edtGlobalDirPath);
        edtFontSize = findViewById(R.id.edtEditorFontSize);

        edtGlobLibsPath.setText(MainActivity.ideConfig.getGlobalDirPath());
        edtFontSize.setText("" + MainActivity.editor.editorConfig.getFontSize());
    }

    public void onBtnSaveClick(View v) {
        String path = edtGlobLibsPath.getText().toString();

        if (!path.endsWith("/")) {
            path += "/";
        }

        MainActivity.editor.setFontSize(Integer.parseInt(edtFontSize.getText().toString()));
        MainActivity.ideConfig.setGlobalDirPath(path);

        finish();
    }
}
