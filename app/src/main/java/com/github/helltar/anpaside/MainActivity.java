package com.github.helltar.anpaside;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import com.github.helltar.anpaside.editor.CodeEditor;
import com.github.helltar.anpaside.ide.IdeConfig;
import com.github.helltar.anpaside.ide.IdeInit;
import com.github.helltar.anpaside.logging.Logger;
import com.github.helltar.anpaside.project.ProjectBuilder;
import com.github.helltar.anpaside.project.ProjectManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT;
import static androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY;
import static com.github.helltar.anpaside.Consts.ASSET_DIR_STUBS;
import static com.github.helltar.anpaside.Consts.DATA_LIB_PATH;
import static com.github.helltar.anpaside.Consts.DATA_PKG_PATH;
import static com.github.helltar.anpaside.Consts.DIR_MAIN;
import static com.github.helltar.anpaside.Consts.DIR_SRC;
import static com.github.helltar.anpaside.Consts.EXT_PAS;
import static com.github.helltar.anpaside.Consts.EXT_PROJ;
import static com.github.helltar.anpaside.Consts.MP3CC;
import static com.github.helltar.anpaside.Utils.fileExists;
import static com.github.helltar.anpaside.Utils.getPathFromUri;
import static com.github.helltar.anpaside.logging.Logger.LMT_ERROR;
import static com.github.helltar.anpaside.logging.Logger.LMT_INFO;

public class MainActivity extends Activity {

    public static CodeEditor editor;
    public static IdeConfig ideConfig;
    private ProjectManager pman = new ProjectManager();

    private static TextView tvLog;
    public static ScrollView svLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLog = findViewById(R.id.tvLog);
        svLog = findViewById(R.id.svLog);

        TabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        editor = new CodeEditor(this, tabHost);
        editor.setBtnTabCloseName(getString(R.string.pmenu_tab_close));

        ideConfig = new IdeConfig(this);

        init();
    }

    private void init() {
        Logger.addLog(getString(R.string.app_name) + " " + getAppVersionName());

        if (!ideConfig.isAssetsInstall()) {
            installAssets();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openFile(editor.editorConfig.getLastProject());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFile(editor.editorConfig.getLastProject());
            } else {
                new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("permission.WRITE_EXTERNAL_STORAGE error")
                    .setPositiveButton("Exit",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            exitApp();
                        }

                    })
                    .show();
            }
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
            lines += "\t\t\t\t\t\t\t\t\t- " + msgLines[i] + "<br>";
        }

        final Spanned text = HtmlCompat.fromHtml(
            new SimpleDateFormat(
                "[HH:mm:ss]: ",
                // Получаем язык пользователя
                Resources.getSystem().getConfiguration().getLocales().get(0)).format(new Date()) + "<font color='" + fontColor + "'>"
            + msgLines[0].replace("\n", "<br>") + "</font><br>"
            + lines,
            // TODO В новом HtmlCompat есть такая интересная вещь, как флаги
            // TODO стоит это изучить
            FROM_HTML_MODE_COMPACT
        );

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
            showAlertMsg(R.string.dlg_title_invalid_value, String.format(getString(R.string.err_project_name_least_chars), 3));
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

    private void createModule(String moduleName) {
        if (moduleName.length() < 3) {
            showAlertMsg(R.string.dlg_title_invalid_value, String.format(getString(R.string.err_module_name_least_chars), 3));
            return;
        }

        final String filename = pman.getProjectPath() + DIR_SRC + moduleName + EXT_PAS;

        if (!fileExists(filename)) {
            if (pman.createModule(filename)) {
                openFile(filename);
            }
        } else {
            new AlertDialog.Builder(this)
                .setMessage(R.string.err_module_exists)
                .setNegativeButton(R.string.dlg_btn_cancel, null)
                .setPositiveButton(R.string.dlg_btn_rewrite,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (new File(filename).delete()) {
                            if (pman.createModule(filename)) {
                                openFile(filename);
                            }
                        } else {
                            Logger.addLog(getString(R.string.err_del_old_module) + ": " + filename, LMT_ERROR);
                        }
                    }})
                .show();
        }
    }

    private boolean openFile(String filename) {
        if (fileExists(filename, true)) {
            if (isProjectFile(filename) && pman.openProject(filename)) {
                editor.editorConfig.setLastProject(filename);
                filename = pman.getMainModuleFilename();
            }

            if (editor.openFile(filename)) {
                editor.editorConfig.setLastFilename(filename);
                return true;
            }
        }

        return false;
    }

    private boolean isProjectFile(String filename) {
        return FilenameUtils.getExtension(filename).equals(EXT_PROJ.substring(1, EXT_PROJ.length()));
    }

    private View getViewById(int resource) {
        return this.getLayoutInflater().inflate(resource, null);
    }

    private void showNewProjectDialog() {
        View view = getViewById(R.layout.dialog_new_project);

        final EditText edtProjectsDir = view.findViewById(R.id.edtProjectsDir);
        final EditText edtProjectName = view.findViewById(R.id.edtProjectName);

        edtProjectsDir.setText(DIR_MAIN);

        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_title_new_project)
            .setView(view)
            .setNegativeButton(R.string.dlg_btn_cancel, null)
            .setPositiveButton(R.string.dlg_btn_create, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    createProject(edtProjectsDir.getText().toString(), edtProjectName.getText().toString());
                }
            })
            .show();
    }

    private void showProjectConfigDialog() {
        View view = getViewById(R.layout.dialog_project_config);

        final EditText edtMidletName = view.findViewById(R.id.edtMidletName);
        final EditText edtMidletVendor = view.findViewById(R.id.edtMidletVendor);
        final EditText edtMidletVersion = view.findViewById(R.id.edtMidletVersion);

        edtMidletName.setText(pman.getMidletName());
        edtMidletVendor.setText(pman.getMidletVendor());
        edtMidletVersion.setText(pman.getMidletVersion());

        new AlertDialog.Builder(this)
            .setTitle(R.string.manifest_mf)
            .setView(view)
            .setNegativeButton(R.string.dlg_btn_cancel, null)
            .setPositiveButton(R.string.menu_file_save, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    try {
                        pman.setMidletName(edtMidletName.getText().toString());
                        pman.setMidletVendor(edtMidletVendor.getText().toString());
                        pman.setVersion(edtMidletVersion.getText().toString());
                        pman.save(pman.getProjectConfigFilename());
                    } catch (IOException e) {
                        Logger.addLog(e);
                    }
                }
            })
            .show();
    }

    private void showNewModuleDialog() {
        View view = getViewById(R.layout.dialog_new_module);

        final EditText edtModuleName = view.findViewById(R.id.edtNewModuleName);

        new AlertDialog.Builder(this)
            .setTitle(R.string.dlg_title_new_module)
            .setView(view)
            .setPositiveButton(R.string.dlg_btn_create,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    createModule(edtModuleName.getText().toString());
                }

            })
            .setNegativeButton(R.string.dlg_btn_cancel, null)
            .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setView(getViewById(R.layout.dialog_about))
            .setNegativeButton("ОК", null)
            .show();
    }

    private void showAlertMsg(int resId, String msg) {
        showAlertMsg(getString(resId), msg);
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

    private boolean saveAllFiles() {
        return saveAllFiles(true);
    }

    private boolean saveAllFiles(boolean showOkMsg) {
        if (editor.saveAllFiles()) {
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
        menu.findItem(R.id.miFileSave).setEnabled(editor.isFilesModified);
        menu.findItem(R.id.miProjectConfig).setEnabled(pman.isProjectOpen());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.miRun:
                if (pman.isProjectOpen()) {
                    if (saveAllFiles(false)) {
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

            case R.id.miProjectConfig:
                showProjectConfigDialog();
                return true;

            case R.id.miFileOpen:
                showOpenFileDialog();
                return true;

            case R.id.miFileSave:
                saveAllFiles();
                return true;

            case R.id.miSettings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            case R.id.miAbout:
                showAbout();
                return true;

            case R.id.miExit:
                if (editor.isFilesModified) {
                    showExitDialog();
                } else {
                    exitApp();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.menu_exit)
            .setMessage(R.string.dlg_msg_save_modified_files)
            .setPositiveButton(R.string.dlg_btn_yes,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    if (editor.saveAllFiles()) {
                        exitApp();
                    }
                }
            })
            .setNegativeButton(R.string.dlg_btn_no,
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    exitApp();
                }

            })
            .show();
    }

    private void exitApp() {
        finish();
        System.exit(0);
    }

    private void buildProject() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
                boolean result = false;
                ProjectBuilder builder;

                @Override
                public void run() {
                    builder = new ProjectBuilder(
                        pman.getProjectConfigFilename(),
                        DATA_LIB_PATH + MP3CC,
                        DATA_PKG_PATH + ASSET_DIR_STUBS + "/",
                        ideConfig.getGlobalDirPath());

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
