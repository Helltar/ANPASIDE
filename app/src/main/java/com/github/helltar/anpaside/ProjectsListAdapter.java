package com.github.helltar.anpaside;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProjectsListAdapter extends RecyclerView.Adapter<ProjectsListAdapter.ViewHolder> {

    private List<ProjectsList> projectsList;

    public ProjectsListAdapter(List<ProjectsList> contacts) {
        projectsList = contacts;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView tvProjectName;

        public ViewHolder(Context context, View itemView) {
            super(itemView);
            this.tvProjectName = (TextView) itemView.findViewById(R.id.tvProjectName);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();

            if (position != RecyclerView.NO_POSITION) {
                MainActivity.projectFilename =
                        Consts.WORK_DIR_PATH + Consts.DIR_PROJECTS
                                + tvProjectName.getText() + "/"
                                + tvProjectName.getText() + Consts.EXT_PROJ;

                ProjectsListActivity.activity.finish();
            }
        }
    }

    @Override
    public ProjectsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactView = inflater.inflate(R.layout.item_project, parent, false);
        ViewHolder viewHolder = new ViewHolder(context, contactView);
        return viewHolder;
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
