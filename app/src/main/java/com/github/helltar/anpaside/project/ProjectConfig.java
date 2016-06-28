package com.github.helltar.anpaside.project;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ProjectConfig {

    private Properties p = new Properties();

    public void open(String filename) throws IOException {
        p.load(new FileInputStream(filename));
    }

    public void save(String filename) throws IOException {
        p.store(new FileOutputStream(filename), null);
    }

    public void setMainModuleName(String mainModule) {
        p.setProperty("MainModule", mainModule);
    }

    public String getMainModuleName() {
        return p.getProperty("MainModule", "");
    }

    public void setMathType(int mathType) {
        p.setProperty("MathType", Integer.toString(mathType));
    }

    public int getMathType() {
        return Integer.parseInt(p.getProperty("MathType", "0"));
    }

    public void setCanvasType(int canvasType) {
        p.setProperty("CanvasType", Integer.toString(canvasType));
    }

    public int getCanvasType() {
        return Integer.parseInt(p.getProperty("CanvasType", "1"));
    }

    public void setMidletName(String midletName) {
        p.setProperty("Name", midletName);
    }

    public String getMidletName() {
        return p.getProperty("Name", "app");
    }

    public void setMidletVendor(String midletVendor) {
        p.setProperty("Vendor", midletVendor);
    }

    public String getMidletVendor() {
        return p.getProperty("Vendor", "vendor");
    }

    public void setMidletIcon(String midletIcon) {
        p.setProperty("Icon", midletIcon);
    }

    public String getMidletIcon() {
        return p.getProperty("Icon", "/icon.png");
    }

    public void setVersMajor(int versMajor) {
        p.setProperty("Major", Integer.toString(versMajor));
    }

    public int getVersMajor() {
        return Integer.parseInt(p.getProperty("Major", "1"));
    }

    public void setVersMinor(int versMinor) {
        p.setProperty("Minor", Integer.toString(versMinor));
    }

    public int getVersMinor() {
        return Integer.parseInt(p.getProperty("Minor", "0"));
    }

    public void setVersBuild(int versBuild) {
        p.setProperty("Build", Integer.toString(versBuild));
    }

    public int getVersBuild() {
        return Integer.parseInt(p.getProperty("Build", "0"));
    }
}

