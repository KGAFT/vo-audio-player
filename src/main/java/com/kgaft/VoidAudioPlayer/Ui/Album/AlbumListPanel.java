package com.kgaft.VoidAudioPlayer.Ui.Album;

import com.kgaft.VoidAudioPlayer.Verbose.Album;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

class AlbumClickListener implements MouseListener {
    private Album album;
    private IOnAlbumSelected listener = null;

    public AlbumClickListener(Album album) {
        this.album = album;
    }

    public IOnAlbumSelected getListener() {
        return listener;
    }

    public void setListener(IOnAlbumSelected listener) {
        this.listener = listener;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (listener != null) {
            listener.onAlbumSelected(album);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}

public class AlbumListPanel extends JScrollPane {
    private JPanel albumPanel = new JPanel();
    private List<AlbumClickListener> listeners = new ArrayList<AlbumClickListener>();

    public AlbumListPanel(List<Album> albumList) {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        albumPanel.setLayout(new GridLayout(0, 3));
        albumList.forEach(album -> {
            if (album.getName().length() <= 0) {
                System.out.println(album.getArtist());
                album.getTracks().forEach(track -> {
                    System.out.println(track.getName() + track.getAlbumName() + track.getPath() + track.getDurationMs());
                });
            }
            AlbumCard albumCard = new AlbumCard(album);
            AlbumClickListener albumClickListener = new AlbumClickListener(album);
            albumPanel.add(albumCard);
            albumCard.addMouseListener(albumClickListener);
            listeners.add(albumClickListener);
        });
        setViewportView(albumPanel);
        albumPanel.invalidate();
        getVerticalScrollBar().setUnitIncrement(18);
        invalidate();
    }

    public void setOnAlbumSelected(IOnAlbumSelected listener) {
        listeners.forEach(element -> element.setListener(listener));
    }
}
