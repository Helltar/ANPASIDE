package com.github.helltar.anpaside;

import java.io.File;
import java.util.ArrayList;

public class ProjectsList {

    private final String name;

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
        ArrayList<ProjectsList> result = new ArrayList<>();

        File folder = new File(Consts.PROJECTS_DIR_PATH);
        File[] filesInFolder = folder.listFiles();

        if (filesInFolder != null) {
            for (File file : filesInFolder) {
                if (new File(Consts.PROJECTS_DIR_PATH + file.getName() + "/"
                        + file.getName() + Consts.EXT_PROJ).exists()) {
                    result.add(new ProjectsList(file.getName()));
                }
            }
        }

        return result;
    }
}