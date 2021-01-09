package com.github.helltar.anpaside;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import com.github.helltar.anpaside.MainActivity;
import com.github.helltar.anpaside.editor.CodeEditor;
import com.github.helltar.anpaside.editor.EditorConfig;
import com.github.helltar.anpaside.ide.IdeConfig;
import com.github.helltar.anpaside.ide.IdeInit;
import com.github.helltar.anpaside.logging.Logger;
import com.github.helltar.anpaside.project.ProjectBuilder;
import com.github.helltar.anpaside.project.ProjectManager;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import static com.github.helltar.anpaside.Consts.*;
import static com.github.helltar.anpaside.logging.Logger.*;
import static com.github.helltar.anpaside.Utils.*;

public class MainActivity extends Activity {

    private CodeEditor editor;
    private EditorConfig editorConfig;
    private IdeConfig ideConfig;
    private ProjectManager pman = new ProjectManager();

    private static TextView tvLog;
    public static ScrollView svLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        tvLog = findViewById(R.id.tvLog);
        svLog = findViewById(R.id.svLog);

        TabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        editor = new CodeEditor(this, tabHost);
        editor.setBtnTabCloseName(getString(R.string.pmenu_tab_close));

        ideConfig = new IdeConfig(this);
        editorConfig = new EditorConfig(this);

        init();
    }

    private void init() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }

        if (ideConfig.isAssetsInstall()) {
            Logger.addLog(getString(R.string.app_name) + " " + getAppVersionName());
            openFile(editorConfig.getLastFilename());
            openFile(editorConfig.getLastProject());
        } else {
            installAssets();
        }
    }

    public static void addGuiLog(String msg, int msgType) {
        if (msg.isEmpty()) {
            return;
        }

        String fontColor = "#aaaaaa";

        if (msgType == LMT_INFO) {
            fontColor = "#00aa00";
        } else if (msgType == LMT_ERROR) {
            fontColor = "#ee0000";
        }

        String[] msgLines = msg.split("\n");
        String lines = "";

        for (int i = 1; i < msgLines.length; i++) {
            lines += "\t\t\t\t\t\t- " + msgLines[i] + "<br>";
        }

        final Spanned text = Html.fromHtml(new SimpleDateFormat("[HH:mm:ss]: ").format(new Date())
                                           + "<font color='" + fontColor + "'>"
                                           + msgLines[0].replace("\n", "<br>") + "</font><br>"
                                           + lines);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (tvLog.getText().length() > 1024) {
                        tvLog.setText("");
                    }

                    tvLog.append(text);
                    svLog.fullScroll(ScrollView.FOCUS_DOWN);
                    svLog.setVisibility(View.VISIBLE);
                }
            });
    }

    private void showOpenFileDialog() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        startActivityForResult(intent, 1);
    }

    private void startActionViewIntent(String filename) {
        File file = new File(filename);
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.getName()));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), type);

        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            openFile(getPathFromUri(this, data.getData()));
        }
    }

    private void createProject(final String projDir, final String projName) {
        final String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";

        if (projName.length() < 3) {
            showAlertMsg(R.string.dlg_title_invalid_value,
                         String.format(getString(R.string.err_project_name_least_chars), 3));
            return;
        }

        final String projectPath = sdcardPath + projDir + "/" + projName + "/";

        if (!fileExists(projectPath)) {
            if (pman.createProject(sdcardPath + projDir + "/", projName)) {
                openFile(pman.getProjectConfigFilename());
            }
        } else {
            new AlertDialog.Builder(MainActivity.this)
                .setMessage(R.string.err_project_exists)
                .setPositiveButton(R.string.dlg_btn_rewrite,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        try {
                            FileUtils.deleteDirectory(new File(projectPath));
                            if (pman.createProject(sdcardPath + projDir + "/", projName)) {
                                openFile(pman.getProjectConfigFilename());
                            }
                        } catch (IOException ioe) {
                            Logger.addLog(ioe);
                        }
                    }
                })
                .setNegativeButton(R.string.dlg_btn_cancel, null)
                .show();
        }
    }

    private void createModule(String filename) {
        if (pman.createModule(filename)) {
            openFile(filename);
        }
    }

    private boolean openFile(String filename) {
        if (fileExists(filename, true)) {
            if (isProjectFile(filename) && pman.openProject(filename)) {
                editorConfig.setLastProject(filename);
                filename = pman.getMainModuleFilename();
            }

            if (editor.openFile(filename)) {
                editorConfig.setLastFilename(filename);
                return true;
            }
        }

        return false;
    }

    private boolean isProjectFile(String filename) {
        return FilenameUtils.getExtension(filename).equals(EXT_PROJ.substring(1, EXT_PROJ.length()));
    }

    private void showNewProjectDialog() {
        LayoutInflater li = this.getLayoutInflater();
        View view = li.inflate(R.layout.new_project_dialog, null);

        final EditText edtProjectsDir = view.findViewById(R.id.edtProjectsDir);
        final EditText edtProjectName = view.findViewById(R.id.edtProjectName);

        edtProjectsDir.setText(DIR_MAIN);

        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_title_new_project)
            .setMessage("")
            .setView(view)
            .setNegativeButton(R.string.dlg_btn_cancel, null)
            .setPositiveButton(R.string.dlg_btn_create, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    createProject(edtProjectsDir.getText().toString(), edtProjectName.getText().toString());
                }
            })
            .show();
    }

    private void showNewModuleDialog() {
        final EditText edtModuleName = new EditText(this);
        edtModuleName.setHint(R.string.dlg_hint_module_name);

        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_title_new_module)
            .setView(edtModuleName)
            .setPositiveButton(R.string.dlg_btn_create,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String moduleName = edtModuleName.getText().toString();

                    if (moduleName.length() < 3) {
                        showAlertMsg(R.string.dlg_title_invalid_value,
                                     String.format(getString(R.string.err_module_name_least_chars), 3));
                        return;
                    }

                    final String filename = pman.getProjectPath() + DIR_SRC + moduleName + EXT_PAS;

                    if (!fileExists(filename)) {
                        createModule(filename);
                    } else {
                        new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.err_module_exists)
                            .setPositiveButton(R.string.dlg_btn_rewrite,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (new File(filename).delete()) {
                                        createModule(filename);
                                    } else {
                                        Logger.addLog(
                                            getString(R.string.err_del_old_module) + ": " + filename,
                                            LMT_ERROR);
                                    }
                                }
                            })
                            .setNegativeButton(R.string.dlg_btn_cancel, null)
                            .show();
                    }
                }

            })
            .setNegativeButton(R.string.dlg_btn_cancel, null)
            .show();
    }

    private void showAlertMsg(int resId, String msg) {
        showAlertMsg(getString(resId), msg);
    }

    private void showAlertMsg(int resId) {
        showAlertMsg("", getString(resId));
    }

    private void showAlertMsg(String title, String msg) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setNegativeButton("ОК", null)
            .show();
    }

    private void showToastMsg(int resId) {
        showToastMsg(getString(resId));
    }

    private void showToastMsg(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM, 0, 80);
        toast.show();
    }

    private String getAppVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;  
        } catch (PackageManager.NameNotFoundException e) {
            return "null";
        }
    }

    private boolean saveCurrentFile() {
        return saveCurrentFile(true);
    }

    private boolean saveCurrentFile(boolean showOkMsg) {
        if (editor.saveCurrentFile()) {
            if (showOkMsg) {
                showToastMsg(R.string.msg_saved);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.miCreateModule).setEnabled(pman.isProjectOpen());
        menu.findItem(R.id.miFileSave).setEnabled(editor.isCurrentFileModified());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.miRun:
                if (pman.isProjectOpen()) {
                    if (saveCurrentFile(false)) { 
                        buildProject();
                    }
                } else {
                    showToastMsg(R.string.msg_no_open_project);
                }

                return true;

            case R.id.miCreateProject:
                showNewProjectDialog();
                return true;

            case R.id.miCreateModule:
                showNewModuleDialog();
                return true;

            case R.id.miFileOpen:
                showOpenFileDialog();
                return true;

            case R.id.miFileSave:
                saveCurrentFile();
                return true;

            case R.id.miAbout:
                showAlertMsg(R.string.about_text);
                return true;

            case R.id.miExit:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void buildProject() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
                boolean result = false;
                ProjectBuilder builder;

                @Override
                public void run() {
                    builder = new ProjectBuilder(
                        pman.getProjectConfigFilename(),
                        DATA_PKG_PATH + ASSET_DIR_BIN + "/" + MP3CC,
                        DATA_PKG_PATH + ASSET_DIR_STUBS + "/",
                        pman.getProjLibsDir());

                    result = builder.build();

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (result) {
                                    startActionViewIntent(builder.getJarFilename());
                                }
                            }
                        });
                }
            });
    }

    private void installAssets() {
        Logger.addLog(getString(R.string.msg_install_start));

        Executors.newSingleThreadExecutor().execute(new Runnable() {
                boolean result = false;

                @Override
                public void run() {
                    result = new IdeInit(MainApp.getContext().getAssets()).install();

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (result) {
                                    ideConfig.setInstState(true);
                                    Logger.addLog(getString(R.string.msg_install_ok), LMT_INFO);
                                }
                            }
                        });
                }
            });
    }    
}
