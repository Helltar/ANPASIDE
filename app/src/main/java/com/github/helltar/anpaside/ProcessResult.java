package com.github.helltar.anpaside;

public class ProcessResult {

    public boolean started;
    public String output;

    ProcessResult(boolean started, String output) {
        this.started = started;
        this.output = output;
    }
}
