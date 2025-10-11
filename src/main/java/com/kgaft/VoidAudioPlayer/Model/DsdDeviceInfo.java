package com.kgaft.VoidAudioPlayer.Model;

public class DsdDeviceInfo {
    private String description;
    private String name;

    public DsdDeviceInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public DsdDeviceInfo() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
