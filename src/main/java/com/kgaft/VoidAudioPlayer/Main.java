package com.kgaft.VoidAudioPlayer;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import com.kgaft.VoidAudioPlayer.Native.PlayerDsd;
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
        //index.addDirectory("/mnt/files2/Music");
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlayerWindow window = new PlayerWindow(index.getAlbums());

        /*
        List<String> devices = PlayerDsd.enumerateSupportedDevices();
        for (int i = 0; i < devices.size(); i++) {
            System.out.println("id: "+i +" device name: "+devices.get(i));
        }
        Scanner scanner = new Scanner(System.in);
        String name = devices.get(scanner.nextInt());
        name = name.split("/:/")[0];
        System.out.println(name);
        long playerHandle = PlayerDsd.initializePlayer(name);
        PlayerDsd.loadTrack(playerHandle, "/mnt/files2/Music/test.dsf");
        new Thread(() -> {
            PlayerDsd.playOnCurrentThread(playerHandle);
        }).start();
        Thread.sleep(1000);
        PlayerDsd.seekTrack(playerHandle, 0.5f);
        while (true) {

        }

         */
    }
}
