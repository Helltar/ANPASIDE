package com.github.helltar.anpaside;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import com.github.helltar.anpaside.ide.IdeConfig;

import static com.github.helltar.anpaside.Consts.*;

public class SettingsActivity extends Activity {

    private EditText edtGlobLibsPath;
    private IdeConfig ideConfig;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ideConfig = new IdeConfig(this);
        edtGlobLibsPath = findViewById(R.id.edtGlobalDirPath);

        if (ideConfig.getGlobalDirPath().isEmpty()) {
            edtGlobLibsPath.setText(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DIR_MAIN + "/" + DIR_LIBS);
        } else {
            edtGlobLibsPath.setText(ideConfig.getGlobalDirPath());
        }
    }

    public void onBtnSaveClick(View v) {
        ideConfig.setGlobalDirPath(edtGlobLibsPath.getText().toString());
        finish();
    }
}
