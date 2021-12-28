package com.github.helltar.anpaside;

import static com.github.helltar.anpaside.Consts.DIR_MAIN;
import static com.github.helltar.anpaside.Consts.EXT_PROJ;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.github.helltar.anpaside.logging.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ProjectsListActivity extends ListActivity implements AdapterView.OnItemLongClickListener {

    private ArrayList<String> projectsList = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    private String projectsFilesDir = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            projectsFilesDir = getExternalFilesDir(null) + "/" + DIR_MAIN + "/";

            if (Utils.fileExists(projectsFilesDir)) {
                projectsList.addAll(getProjectsList(projectsFilesDir));
            }
        } catch (IOException e) {
            // TODO: 0 projects
            Logger.addLog(e);
        }

        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, projectsList);
        setListAdapter(mAdapter);
    }

    private ArrayList<String> getProjectsList(String dir) throws IOException {
        ArrayList<String> dirList = new ArrayList<String>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dir))) {
            for (Path path : stream) {
                dirList.add(path.getFileName().toString());
            }
        }

        return dirList;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String projectName = l.getItemAtPosition(position).toString();

        MainActivity.getInstance().openFile(
                projectsFilesDir + projectName + "/" + projectName + EXT_PROJ);

        finish();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        /* TODO: remove project
        String selectedItem = parent.getItemAtPosition(position).toString();
        mAdapter.remove(selectedItem);
        mAdapter.notifyDataSetChanged();
        */
        return true;
    }
}