package com.kgaft.VoidAudioPlayer.Model;


import com.google.gson.Gson;
import com.kgaft.VoidAudioPlayer.Verbose.LibraryIndex;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MSettings {
    private static volatile MSettings currentSettings = null;
    public static Path getUserConfigDir(String appName) {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                return Path.of(appData, appName);
            }
            return Path.of(userHome, "AppData", "Roaming", appName);

        } else if (os.contains("mac")) {
            return Path.of(userHome, "Library", "Application Support", appName);

        } else {
            String xdg = System.getenv("XDG_CONFIG_HOME");
            if (xdg != null && !xdg.isBlank()) {
                return Path.of(xdg, appName);
            }
            return Path.of(userHome, ".config", appName);
        }
    }
    public static MSettings readSettingsOrDefault(){
        if(currentSettings == null){
            if(tryLoadSettings()){
                currentSettings.applySettings();
            }
        }
        return currentSettings;
    }

    public static void saveCurrentSettings(){
        Path pathToConfig = getUserConfigDir("void-audio").resolve("settings.json");
        Gson gson = new Gson();
        try(FileWriter writter = new FileWriter(pathToConfig.toAbsolutePath().toFile())) {
            gson.toJson(currentSettings, writter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean tryLoadSettings(){
        Path pathToConfig = getUserConfigDir("void-audio").resolve("settings.json");
        File file = pathToConfig.toFile();
        if(!file.exists()){
            currentSettings = new MSettings();
            return false;
        }
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(pathToConfig.toAbsolutePath().toFile())) {
            currentSettings = gson.fromJson(reader, MSettings.class);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentSettings = new MSettings();
        return false;
    }

    private String collectionDBPath = getUserConfigDir("void-audio").resolve("collection.db").toAbsolutePath().toString();
    private String defaultDsdPlaybackDeviceName;
    private String defaultGstPlaybackDevice;

    public List<String> showAvailableGstDevices(){
        return MPlayer.enumerateDevices();
    }

    public void setGstDevice(String device){
        MPlayer.pickDevice(device);
        this.defaultGstPlaybackDevice = device;
    }

    public List<String> showAvailableDsdDevices(){
        List<DsdDeviceInfo> devices = MPlayer.enumerateDsdDevices();
        List<String> result = new ArrayList<>();
        devices.forEach(device -> {
            result.add(device.getDescription()+"/:/"+device.getName());
        });
        return result;
    }
    public void setDsdDevice(String device){
        String[] args = device.split("/:/");
        DsdDeviceInfo info = new DsdDeviceInfo(args[1], args[0]);
        MPlayer.initDsdDevice(info);
        this.defaultDsdPlaybackDeviceName = device;
    }
    private void applySettings(){
        if(!this.defaultDsdPlaybackDeviceName.isEmpty()){
            setDsdDevice(this.defaultDsdPlaybackDeviceName);
        }
        if(!this.defaultGstPlaybackDevice.isEmpty()){
            setGstDevice(this.defaultGstPlaybackDevice);
        }
        if(!this.collectionDBPath.isEmpty()){
            setCollectionDBPath(this.collectionDBPath);
        }
    }

    public String getCollectionDBPath() {
        return collectionDBPath;
    }

    public void setCollectionDBPath(String collectionDBPath) {
        this.collectionDBPath = collectionDBPath;
        LibraryIndex.setDatabaseUrl(collectionDBPath);
    }

    public String getDefaultDsdPlaybackDeviceName() {
        return defaultDsdPlaybackDeviceName;
    }

    public String getDefaultGstPlaybackDevice() {
        return defaultGstPlaybackDevice;
    }
}
