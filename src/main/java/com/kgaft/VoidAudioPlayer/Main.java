package com.kgaft.VoidAudioPlayer;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import com.kgaft.VoidAudioPlayer.Verbose.*;
import javax.swing.*;
import java.io.File;

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

    public static void main(String[] args) {
        LibraryIndex index = LibraryIndex.getInstance();
       //  index.addDirectory("F:/Music");
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlayerWindow window = new PlayerWindow(index.getAlbums());

    }
}
