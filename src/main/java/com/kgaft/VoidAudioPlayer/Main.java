package com.kgaft.VoidAudioPlayer;


import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.kgaft.VoidAudioPlayer.Model.DsdDeviceInfo;
import com.kgaft.VoidAudioPlayer.Model.MPlayer;
import com.kgaft.VoidAudioPlayer.Native.CueParser;
import com.kgaft.VoidAudioPlayer.Verbose.*;
import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Scanner;

//Testing purposes only, in future will be organised!

public class Main {
    static {
        File file = new File("target/debug");
        for (File listFile : file.listFiles()) {
            if (listFile.getName().endsWith(".so") || listFile.getName().endsWith(".dylib") || listFile.getName().endsWith(".dll")) {
                try {
                    System.load(listFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {

        LibraryIndex index = LibraryIndex.getInstance();
        // index.addDirectory("/mnt/files2/Music");
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }


        List<DsdDeviceInfo> devices = MPlayer.enumerateDsdDevices();
        for (int i = 0; i < devices.size(); i++) {
            System.out.println("id: "+i +" device name: "+devices.get(i).getDescription());
        }
        System.out.println("Select dsd device: ");
        Scanner scanner = new Scanner(System.in);
        DsdDeviceInfo name = devices.get(scanner.nextInt());
        MPlayer.initDsdDevice(name);
        PlayerWindow window = new PlayerWindow(index.getAlbums());


    }
}
