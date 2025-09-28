package com.kgaft.VoidAudioPlayer;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.kgaft.VoidAudioPlayer.Native.FileDialog;
import com.kgaft.VoidAudioPlayer.Native.LibrarySerializer;
import com.kgaft.VoidAudioPlayer.Native.Track;
import com.kgaft.VoidAudioPlayer.Ui.Album.AlbumInfoPanel;
import com.kgaft.VoidAudioPlayer.Ui.Album.AlbumListPanel;
import com.kgaft.VoidAudioPlayer.Ui.Album.IOnAlbumSelected;
import com.kgaft.VoidAudioPlayer.Ui.Album.IOnTrackSelected;
import com.kgaft.VoidAudioPlayer.Ui.PlayerPanel;

import com.kgaft.VoidAudioPlayer.Ui.TrackPanel;
import com.kgaft.VoidAudioPlayer.Verbose.Album;
import com.kgaft.VoidAudioPlayer.Verbose.LibraryParser;


import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


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
        List<Album> albumList = new ArrayList<>();
        File indexFile = new File("index.bin");
        if (indexFile.exists()) {
            albumList = LibrarySerializer.deserializeFromFile(indexFile.getAbsolutePath());
        } else {
            LibraryParser.recurrentIterDirectory("F:/Music", albumList);
            albumList.sort(Comparator.comparing(Album::getName).reversed());
            LibrarySerializer.serializeAndSaveToFile(albumList, indexFile.getAbsolutePath());
        }
        albumList.forEach(album -> {
            System.out.println(album.getName());
        });
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlayerWindow window = new PlayerWindow(albumList);

    }
}