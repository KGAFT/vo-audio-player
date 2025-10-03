package com.kgaft.VoidAudioPlayer;

import com.kgaft.VoidAudioPlayer.Verbose.Track;
import com.kgaft.VoidAudioPlayer.Ui.Album.AlbumInfoPanel;
import com.kgaft.VoidAudioPlayer.Ui.Album.AlbumListPanel;
import com.kgaft.VoidAudioPlayer.Ui.Album.IOnAlbumSelected;
import com.kgaft.VoidAudioPlayer.Ui.Album.IOnTrackSelected;
import com.kgaft.VoidAudioPlayer.Ui.PlayerPanel;
import com.kgaft.VoidAudioPlayer.Ui.TrackPanel;
import com.kgaft.VoidAudioPlayer.Verbose.Album;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class PlayerWindow extends JFrame implements IOnAlbumSelected, ActionListener, IOnTrackSelected {
    private List<Album> albumList = new ArrayList<>();
    private TrackPanel trackPanel;
    private AlbumListPanel albumListPanel;
    private PlayerPanel playerPanel = new PlayerPanel(200, 50);
    private boolean isAlbumList = true;
    private AlbumInfoPanel albumInfoPanel;
    private Album currentAlbum = null;
    private boolean firstLoad = true;

    public PlayerWindow(List<Album> albums) {
        super("VoidAudioPlayer");
        this.albumList = albums;
        trackPanel = new TrackPanel(albumList.getFirst().getTracks().stream().filter(t -> true).findFirst().get());
        albumListPanel = new AlbumListPanel(albumList);
        setSize(1280, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(trackPanel, BorderLayout.CENTER);
        add(playerPanel, BorderLayout.SOUTH);
        add(albumListPanel, BorderLayout.WEST);
        albumListPanel.setOnAlbumSelected(this);
        setVisible(true);
    }

    @Override
    public void onAlbumSelected(Album album) {
        if (currentAlbum != album) {
            if (isAlbumList) {
                remove(albumListPanel);
                albumInfoPanel = new AlbumInfoPanel(album);
                albumInfoPanel.onTrackSelected(this);
                albumInfoPanel.setOnBackButtonPressed(this);
                add(albumInfoPanel, BorderLayout.WEST);
                this.currentAlbum = album;
                isAlbumList = false;
            }
            firstLoad = true;
            invalidate();
            validate();
            repaint();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isAlbumList) {
            remove(albumInfoPanel);
            add(albumListPanel, BorderLayout.WEST);
            isAlbumList = true;
        }
        invalidate();
        validate();
        repaint();
    }

    @Override
    public void onTrackSelected(Track track) {
        if (firstLoad || track.getOffsetMs() == 0) {
            playerPanel.loadTrack("\\" + track.getPath());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            firstLoad = false;
        }
        if (track.getOffsetMs() != 0) {
            float offsetMs = track.getOffsetMs();
            float durationMs = track.getDurationMs();
            float offset = (offsetMs/durationMs);
            playerPanel.seekTrack(offset);
        }
        remove(trackPanel);
        trackPanel = new TrackPanel(track);
        add(trackPanel, BorderLayout.CENTER);
        invalidate();
        validate();
        // playerPanel.seekTrack();
    }
}
