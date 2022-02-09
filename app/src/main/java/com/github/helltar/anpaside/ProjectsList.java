package com.github.helltar.anpaside;

import java.io.File;
import java.util.ArrayList;

public class ProjectsList {

    private String name;

    public ProjectsList(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static boolean isProjectsListEmpty() {
        return !createProjectsList().isEmpty();
    }

    public static ArrayList<ProjectsList> createProjectsList() {
        ArrayList<ProjectsList> result = new ArrayList<ProjectsList>();

        try {
            File folder = new File(Consts.WORK_DIR_PATH + Consts.DIR_PROJECTS);
            File[] filesInFolder = folder.listFiles();

            for (File file : filesInFolder) {
                result.add(new ProjectsList(file.getName()));
            }
        } catch (RuntimeException e) {
            // Logger.addLog(e);
        }

        return result;
    }
}