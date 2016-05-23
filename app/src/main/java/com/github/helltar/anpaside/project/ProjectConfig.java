package com.github.helltar.anpaside.project;

import java.io.File;
import java.io.IOException;
import org.ini4j.Wini;

public class ProjectConfig {

    private Wini ini;

    private final String SECTION_MAIN = "MAIN";
    private final String SECTION_MANIFEST = "MANIFEST";
    private final String SECTION_VERSIONS = "VERSIONS";

    public ProjectConfig(String filename) throws IOException {
        File file = new File(filename);

        if (!file.exists()) {
            file.createNewFile();
        }

        ini = new Wini(file);
    }

    public void save() throws IOException {
        ini.store();
    }

    public void setMainModuleName(String val) {
        ini.put(SECTION_MAIN, "MainModule", val);
    }

    public String getMainModuleName() {
        return ini.get(SECTION_MAIN, "MainModule");
    }

    public void setMathType(int val) {
        ini.put(SECTION_MAIN, "MathType", val);
    }

    public int getMathType() {
        return ini.get(SECTION_MAIN, "MathType", int.class);
    }

    public void setCanvasType(int val) {
        ini.put(SECTION_MAIN, "CanvasType", val);
    }

    public int getCanvasType() {
        return ini.get(SECTION_MAIN, "CanvasType", int.class);
    }

    public void setMidletName(String val) {
        ini.put(SECTION_MANIFEST, "Name", val);
    }

    public String getMidletName() {
        return ini.get(SECTION_MANIFEST, "Name");
    }

    public void setMidletVendor(String val) {
        ini.put(SECTION_MANIFEST, "Vendor", val);
    }

    public String getMidletVendor() {
        return ini.get(SECTION_MANIFEST, "Vendor");
    }

    public void setMidletIcon(String val) {
        ini.put(SECTION_MANIFEST, "Icon", val);
    }

    public String getMidletIcon() {
        return ini.get(SECTION_MANIFEST, "Icon");
    }

    public void setVersMajor(int val) {
        ini.put(SECTION_VERSIONS, "Major", val);
    }

    public int getVersMajor() {
        return ini.get(SECTION_VERSIONS, "Major", int.class);
    }

    public void setVersMinor(int val) {
        ini.put(SECTION_VERSIONS, "Minor", val);
    }

    public int getVersMinor() {
        return ini.get(SECTION_VERSIONS, "Minor", int.class);
    }

    public void setVersBuild(int val) {
        ini.put(SECTION_VERSIONS, "Build", val);
    }

    public int getVersBuild() {
        return ini.get(SECTION_VERSIONS, "Build", int.class);
    }
}

