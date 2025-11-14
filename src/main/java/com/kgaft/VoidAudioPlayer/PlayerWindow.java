package com.kgaft.VoidAudioPlayer;

import com.kgaft.VoidAudioPlayer.Ui.PlaylistPanel;
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
import java.util.stream.Collectors;

public class PlayerWindow extends JFrame implements IOnAlbumSelected, ActionListener, IOnTrackSelected {
    private List<Album> albumList = new ArrayList<>();
    private TrackPanel trackPanel;
    private AlbumListPanel albumListPanel;
    private PlayerPanel playerPanel = new PlayerPanel(200, 50);
    private PlaylistPanel playlistPanel = new PlaylistPanel();
    private boolean isAlbumList = true;
    private AlbumInfoPanel albumInfoPanel;
    private Album currentAlbum = null;
    private boolean firstLoad = true;

    public PlayerWindow(List<Album> albums) {
        super("VoidAudioPlayer");
        this.albumList = albums;
        trackPanel = new TrackPanel(albumList.get(36).getTracks().stream().filter(t -> true).findFirst().get());
        albumListPanel = new AlbumListPanel(albumList);
        setSize(900, 720);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(trackPanel, BorderLayout.CENTER);
        add(playerPanel, BorderLayout.SOUTH);
        add(albumListPanel, BorderLayout.WEST);
        add(playlistPanel, BorderLayout.EAST);
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
            playerPanel.loadTrack(track);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            firstLoad = false;
        }
        remove(trackPanel);
        trackPanel = new TrackPanel(track);
        add(trackPanel, BorderLayout.CENTER);
        playlistPanel.updateTracks();
        invalidate();
        validate();
        // playerPanel.seekTrack();
    }
}
