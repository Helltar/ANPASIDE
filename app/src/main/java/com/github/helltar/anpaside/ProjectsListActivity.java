package com.github.helltar.anpaside;

import android.app.Activity;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ProjectsListActivity extends AppCompatActivity {

    public static Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);

        activity = this;

        RecyclerView rvProjects = (RecyclerView) findViewById(R.id.rvProjects);

        ArrayList<ProjectsList> projectsList = ProjectsList.createProjectsList();
        ProjectsListAdapter adapter = new ProjectsListAdapter(projectsList);

        rvProjects.setAdapter(adapter);
        rvProjects.setLayoutManager(new LinearLayoutManager(this));

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        rvProjects.addItemDecoration(itemDecoration);
    }
}
