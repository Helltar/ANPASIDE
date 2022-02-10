package com.github.helltar.anpaside.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.helltar.anpaside.Consts;
import com.github.helltar.anpaside.ProjectsList;
import com.github.helltar.anpaside.R;
import com.github.helltar.anpaside.activities.MainActivity;
import com.github.helltar.anpaside.activities.ProjectsListActivity;

import java.util.List;

public class ProjectsListAdapter extends RecyclerView.Adapter<ProjectsListAdapter.ViewHolder> {

    private final List<ProjectsList> projectsList;

    public ProjectsListAdapter(List<ProjectsList> contacts) {
        projectsList = contacts;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView tvProjectName;

        public ViewHolder(View itemView) {
            super(itemView);
            this.tvProjectName = (TextView) itemView.findViewById(R.id.tvProjectName);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            if (position != RecyclerView.NO_POSITION) {
                MainActivity.projectFilename =
                        Consts.PROJECTS_DIR_PATH
                                + tvProjectName.getText()
                                + "/"
                                + tvProjectName.getText()
                                + Consts.EXT_PROJ;

                ProjectsListActivity.activity.finish();
            }
        }
    }

    @NonNull
    @Override
    public ProjectsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View contactView = inflater.inflate(R.layout.item_project, parent, false);
        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(ProjectsListAdapter.ViewHolder holder, int position) {
        ProjectsList project = projectsList.get(position);
        TextView textView = holder.tvProjectName;
        textView.setText(project.getName());
    }

    @Override
    public int getItemCount() {
        return projectsList.size();
    }
}
