package com.kgaft.VoidAudioPlayer;

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

class AlbumPlacingCallback implements IOnAlbumSelected, ActionListener, IOnTrackSelected {

    private JFrame frame;
    private Album currentAlbum = null;
    private AlbumListPanel albumListPanel;
    private AlbumInfoPanel albumInfoPanel;
    private boolean isAlbumList = true;
    private PlayerPanel playerPanel;
    private TrackPanel trackPanel;

    public AlbumPlacingCallback(JFrame frame, AlbumListPanel albumListPanel, PlayerPanel playerPanel, TrackPanel trackPanel) {
        this.frame = frame;
        this.albumListPanel = albumListPanel;
        this.playerPanel = playerPanel;
        this.trackPanel = trackPanel;
    }

    @Override
    public void onAlbumSelected(Album album) {
        if (currentAlbum != album) {
            if (isAlbumList) {
                frame.remove(albumListPanel);
                albumInfoPanel = new AlbumInfoPanel(album);
                albumInfoPanel.onTrackSelected(this);
                albumInfoPanel.setOnBackButtonPressed(this);
                frame.add(albumInfoPanel, BorderLayout.WEST);
                isAlbumList = false;
            }
            frame.invalidate();
            frame.validate();
            frame.repaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isAlbumList) {
            frame.remove(albumInfoPanel);
            frame.add(albumListPanel, BorderLayout.WEST);
            isAlbumList = true;
        }
        frame.invalidate();
        frame.validate();
        frame.repaint();
    }

    @Override
    public void onTrackSelected(Track track) {
        playerPanel.loadTrack("\\" + track.getPath());
        frame.remove(trackPanel);
        trackPanel = new TrackPanel(track);
        frame.add(trackPanel, BorderLayout.CENTER);
        frame.invalidate();
        frame.validate();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // playerPanel.seekTrack();
    }
}

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
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        TrackPanel trackPanel = new TrackPanel(albumList.get(5).getTracks().get(0));
        AlbumListPanel albumListPanel = new AlbumListPanel(albumList);
        PlayerPanel playerPanel = new PlayerPanel(200, 50);
        JFrame frame = new JFrame("VoidAudioPlayer");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(trackPanel, BorderLayout.CENTER);
        frame.add(playerPanel, BorderLayout.SOUTH);

        frame.add(albumListPanel, BorderLayout.WEST);
        albumListPanel.setOnAlbumSelected(new AlbumPlacingCallback(frame, albumListPanel, playerPanel, trackPanel));
        frame.setVisible(true);
    }
}