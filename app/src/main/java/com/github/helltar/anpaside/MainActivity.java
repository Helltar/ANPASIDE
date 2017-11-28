package com.github.helltar.anpaside;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
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
    private static ScrollView svLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLog = (TextView) findViewById(R.id.tvLog);
        svLog = (ScrollView) findViewById(R.id.svLog);

        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();

        editor = new CodeEditor(this, tabHost);
        editor.setBtnTabCloseName(getString(R.string.pmenu_tab_close));

        ideConfig = new IdeConfig(this);
        editorConfig = new EditorConfig(this);

        init();
    }

    private void init() {
        if (ideConfig.isAssetsInstall()) {
            Logger.addLog(getString(R.string.app_name) + " " + getAppVersionName());
            openFile(editorConfig.getLastFilename());
            openFile(editorConfig.getLastProject());
        } else {
            new Install().execute();
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
                }
            });
    }

    private void showOpenFileDialog() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            openFile(data.getData().getPath());
        }
    }

    private void createProject(String path, String name) {
        if (pman.createProject(path, name)) {
            openFile(pman.getProjectConfigFilename());
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
        final EditText edtProjectName = new EditText(this);
        edtProjectName.setHint(R.string.dlg_hint_project_name);

        String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        final String projectsPath = sdcardPath + DIR_MAIN;

        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_title_new_project)
            .setMessage(String.format(getString(R.string.dlg_subtitle_new_project), projectsPath))
            .setView(edtProjectName)
            .setPositiveButton(R.string.dlg_btn_create,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    final String projName = edtProjectName.getText().toString();

                    if (projName.length() < 3) {
                        showAlertMsg(R.string.dlg_title_invalid_value,
                                     String.format(getString(R.string.err_project_name_least_chars), 3));
                        return;
                    }

                    final String projPath = projectsPath + projName;

                    if (!mkdir(projectsPath)) {
                        return;
                    }

                    if (!fileExists(projPath)) {
                        createProject(projectsPath, projName);
                    } else {
                        new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.err_project_exists)
                            .setPositiveButton(R.string.dlg_btn_rewrite,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    try {
                                        FileUtils.deleteDirectory(new File(projPath));
                                        createProject(projectsPath, projName);
                                    } catch (IOException ioe) {
                                        Logger.addLog(ioe);
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
                        new BuildProj().execute();
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

    private void startActionViewIntent(String filename) {
        File file = new File(filename);

        String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);

        if (type == null) {
            type = "*/*";
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), type);

        startActivity(intent);
    }

    private class Install extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Logger.addLog(getString(R.string.msg_install_start));
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return new IdeInit(MainApp.getContext().getAssets()).install();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                ideConfig.setInstState(true);
                Logger.addLog(getString(R.string.msg_install_ok), LMT_INFO);
            }
        }
    }

    private class BuildProj extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params)  {
            ProjectBuilder builder = new ProjectBuilder(pman.getProjectConfigFilename(),
                                                        DATA_PKG_PATH + ASSET_DIR_BIN + "/" + MP3CC,
                                                        DATA_PKG_PATH + ASSET_DIR_STUBS + "/",
                                                        pman.getProjLibsDir());

            if (builder.build()) {
                startActionViewIntent(builder.getJarFilename());
            }

            return null;
        }
    }
}

