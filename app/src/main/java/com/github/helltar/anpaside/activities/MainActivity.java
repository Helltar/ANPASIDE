package com.github.helltar.anpaside.activities;

import static com.github.helltar.anpaside.Consts.ASSETS_STATUS;
import static com.github.helltar.anpaside.Consts.ASSET_DIR_STUBS;
import static com.github.helltar.anpaside.Consts.DATA_LIB_PATH;
import static com.github.helltar.anpaside.Consts.DATA_PKG_PATH;
import static com.github.helltar.anpaside.Consts.DIR_SRC;
import static com.github.helltar.anpaside.Consts.EXT_PAS;
import static com.github.helltar.anpaside.Consts.EXT_PROJ;
import static com.github.helltar.anpaside.Consts.MP3CC;
import static com.github.helltar.anpaside.Consts.PROJECTS_DIR_PATH;
import static com.github.helltar.anpaside.Consts.RCODE_SETTINGS;
import static com.github.helltar.anpaside.Utils.fileExists;
import static com.github.helltar.anpaside.logging.Logger.LMT_ERROR;
import static com.github.helltar.anpaside.logging.Logger.LMT_INFO;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;

import com.github.helltar.anpaside.BuildConfig;
import com.github.helltar.anpaside.ProjectsList;
import com.github.helltar.anpaside.R;
import com.github.helltar.anpaside.editor.CodeEditor;
import com.github.helltar.anpaside.editor.EditorConfig;
import com.github.helltar.anpaside.ide.IdeConfig;
import com.github.helltar.anpaside.ide.IdeInit;
import com.github.helltar.anpaside.logging.Logger;
import com.github.helltar.anpaside.project.ProjectBuilder;
import com.github.helltar.anpaside.project.ProjectManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    protected CodeEditor editor;
    private IdeConfig ideConfig;
    private final ProjectManager projManager = new ProjectManager();

    private static MainActivity activity;
    public static String projectFilename = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        activity = this;

        TabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        editor = new CodeEditor(this, tabHost);
        ideConfig = new IdeConfig(this);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (projManager.isProjectOpen()) {
            openFile(projectFilename);
        }
    }

    private final ActivityResultLauncher<Intent> activityResultLaunch = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RCODE_SETTINGS) {
                    if (result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        EditorConfig editorConfig = new EditorConfig(this);
                        editor.setFontSize(extras.getInt(editorConfig.FONT_SIZE));
                        editor.setHighlighterEnabled(extras.getBoolean(editorConfig.HIGHLIGHTER_ENABLED));
                        editor.setWordwrap(extras.getBoolean(editorConfig.WORDWRAP));
                    }
                }
            });

    private void init() {
        Logger.addLog(getString(R.string.app_name) + " " + getAppVersionName());

        if (ideConfig.isAssetsInstall()) {
            if (!ideConfig.isAssetsUpdate(ASSETS_STATUS)) {
                if (new IdeInit(getAssets()).updateAssets()) {
                    ideConfig.setUpdateAssetsState(ASSETS_STATUS);
                }
            }

            editor.openRecentFiles();
            openFile(editor.editorConfig.getLastProject());
        } else {
            installAssets();
        }
    }

    private void installAssets() {
        Logger.addLog(getString(R.string.msg_install_start));

        if (new IdeInit(getAssets()).install()) {
            ideConfig.setInstState(true);
            ideConfig.setUpdateAssetsState(ASSETS_STATUS);
            Logger.addLog(getString(R.string.msg_install_ok), LMT_INFO);
        }
    }

    public static void addLogToGUI(Spanned text) {
        TextView tvLog = activity.findViewById(R.id.tvLog);
        ScrollView svLog = activity.findViewById(R.id.svLog);

        new Handler(Looper.getMainLooper()).post(() -> {
            if (tvLog.getText().length() > 1024) {
                tvLog.setText("");
            }

            tvLog.append(text);
            svLog.fullScroll(ScrollView.FOCUS_DOWN);
            svLog.setVisibility(View.VISIBLE);
        });
    }

    private void startActionViewIntent(String filename) {
        File file = new File(filename);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);
        intent.setDataAndType(uri, "application/java-archive");

        try {
            startActivity(intent);
        } catch (RuntimeException e) {
            j2meloaderDialog();
        }
    }

    private void j2meloaderDialog() {
        final String id = "ru.playsoftware.j2meloader";

        new AlertDialog.Builder(this)
                .setTitle("J2ME emulator")
                .setView(getViewById(R.layout.dialog_j2meloader))
                .setNegativeButton("Google Play",
                        (dialog, whichButton) -> {
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=" + id)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=" + id)));
                            }
                        })
                .setPositiveButton("F-Droid",
                        (dialog, whichButton) -> startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://f-droid.org/packages/" + id))))
                .setNeutralButton(R.string.dlg_btn_cancel, null)
                .show();
    }

    private void createProject(final String projName) {
        String path = PROJECTS_DIR_PATH;

        if (projName.length() < 3) {
            showAlertMsg(R.string.dlg_title_invalid_value, String.format(getString(R.string.err_project_name_least_chars), 3));
            return;
        }

        final String projectPath = path + projName + "/";

        if (!fileExists(projectPath)) {
            if (projManager.createProject(path, projName)) {
                openFile(projManager.getProjectConfigFilename());
            }
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.err_project_exists)
                    .setPositiveButton(R.string.dlg_btn_rewrite,
                            (dialog, whichButton) -> {
                                try {
                                    FileUtils.deleteDirectory(new File(projectPath));
                                    if (projManager.createProject(path, projName)) {
                                        openFile(projManager.getProjectConfigFilename());
                                    }
                                } catch (IOException ioe) {
                                    Logger.addLog(ioe);
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

        final String filename = projManager.getProjectPath() + DIR_SRC + moduleName + EXT_PAS;

        if (!fileExists(filename)) {
            if (projManager.createModule(filename)) {
                openFile(filename);
            }
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.err_module_exists)
                    .setNegativeButton(R.string.dlg_btn_cancel, null)
                    .setPositiveButton(R.string.dlg_btn_rewrite,
                            (dialog, whichButton) -> {
                                if (new File(filename).delete()) {
                                    if (projManager.createModule(filename)) {
                                        openFile(filename);
                                    }
                                } else {
                                    Logger.addLog(getString(R.string.err_del_old_module) + ": " + filename, LMT_ERROR);
                                }
                            })
                    .show();
        }
    }

    public void openFile(String filename) {
        if (!filename.isEmpty()) {
            if (isProjectFile(filename)) {
                if (projManager.openProject(filename)) {
                    editor.editorConfig.setLastProject(filename);
                    filename = projManager.getMainModuleFilename();
                }
            }

            editor.openFile(filename);
        }
    }

    private boolean isProjectFile(String filename) {
        return FilenameUtils.getExtension(filename).equals(EXT_PROJ.substring(1));
    }

    private View getViewById(int resource) {
        return this.getLayoutInflater().inflate(resource, null);
    }

    private void showNewProjectDialog() {
        View view = getViewById(R.layout.dialog_new_project);

        final EditText edtProjectName = view.findViewById(R.id.edtProjectName);
        TextView tvHomeDir = view.findViewById(R.id.tvHomeDir);
        tvHomeDir.setText(PROJECTS_DIR_PATH);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dlg_title_new_project)
                .setView(view)
                .setNegativeButton(R.string.dlg_btn_cancel, null)
                .setPositiveButton(R.string.dlg_btn_create,
                        (dialog, whichButton) -> createProject(edtProjectName.getText().toString()))
                .show();
    }

    private void showProjectConfigDialog() {
        View view = getViewById(R.layout.dialog_project_config);

        final EditText edtMidletName = view.findViewById(R.id.edtMidletName);
        final EditText edtMidletVendor = view.findViewById(R.id.edtMidletVendor);
        final EditText edtMidletVersion = view.findViewById(R.id.edtMidletVersion);

        edtMidletName.setText(projManager.getMidletName());
        edtMidletVendor.setText(projManager.getMidletVendor());
        edtMidletVersion.setText(projManager.getMidletVersion());

        new AlertDialog.Builder(this)
                .setTitle(R.string.manifest_mf)
                .setView(view)
                .setNegativeButton(R.string.dlg_btn_cancel, null)
                .setPositiveButton(R.string.menu_file_save, (dialog, whichButton) -> {
                    try {
                        projManager.setMidletName(edtMidletName.getText().toString());
                        projManager.setMidletVendor(edtMidletVendor.getText().toString());
                        projManager.setVersion(edtMidletVersion.getText().toString());
                        projManager.save(projManager.getProjectConfigFilename());
                    } catch (IOException e) {
                        Logger.addLog(e);
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
                        (dialog, whichButton) -> createModule(edtModuleName.getText().toString()))
                .setNegativeButton(R.string.dlg_btn_cancel, null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name) + " - " + getAppVersionName() + "." + BuildConfig.VERSION_CODE)
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

    private String getAppVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.miCreateModule).setEnabled(projManager.isProjectOpen());
        menu.findItem(R.id.miFileSave).setEnabled(CodeEditor.isFilesModified);
        menu.findItem(R.id.miProjectConfig).setEnabled(projManager.isProjectOpen());
        menu.findItem(R.id.miFileOpen).setEnabled(ProjectsList.isProjectsListEmpty());
        return super.onPrepareOptionsMenu(menu);
    }

    private void buildProject() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            boolean result = false;
            ProjectBuilder builder;

            @Override
            public void run() {
                builder = new ProjectBuilder(
                        projManager.getProjectConfigFilename(),
                        DATA_LIB_PATH + MP3CC,
                        DATA_PKG_PATH + ASSET_DIR_STUBS + "/",
                        ideConfig.getGlobalDirPath());

                result = builder.build();

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result) {
                        try {
                            startActionViewIntent(builder.getJarFilename());
                        } catch (Exception e) {
                            Logger.addLog(e);
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.miRun) {
            if (projManager.isProjectOpen()) {
                if (editor.saveAllFiles()) {
                    buildProject();
                }
            } else {
                Toast toast = Toast.makeText(this, R.string.msg_no_open_project, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.BOTTOM, 0, 80);
                toast.show();
            }
        } else if (id == R.id.miCreateProject) {
            showNewProjectDialog();
        } else if (id == R.id.miCreateModule) {
            showNewModuleDialog();
        } else if (id == R.id.miProjectConfig) {
            showProjectConfigDialog();
        } else if (id == R.id.miFileOpen) {
            startActivity(new Intent(this, ProjectsListActivity.class));
        } else if (id == R.id.miFileSave) {
            editor.saveAllFiles(true);
        } else if (id == R.id.miSettings) {
            activityResultLaunch.launch(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.miDocs) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://helltar.com/mpascal/docs/")));
        } else if (id == R.id.miAbout) {
            showAbout();
        } else if (id == R.id.miExit) {
            if (CodeEditor.isFilesModified) {
                showExitDialog();
            } else {
                exitApp();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_exit)
                .setMessage(R.string.dlg_msg_save_modified_files)
                .setPositiveButton(R.string.dlg_btn_yes,
                        (dialog, whichButton) -> {
                            if (editor.saveAllFiles()) {
                                exitApp();
                            }
                        })
                .setNegativeButton(R.string.dlg_btn_no,
                        (dialog, whichButton) -> exitApp())
                .show();
    }

    private void exitApp() {
        finish();
        System.exit(0);
    }
}