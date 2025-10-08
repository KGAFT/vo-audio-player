package com.kgaft.VoidAudioPlayer.Ui.Album;

import com.kgaft.VoidAudioPlayer.Verbose.Album;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

public class AlbumListPanel extends JScrollPane implements IOnAlbumSelected {
    private JPanel albumPanel = new JPanel();
    private volatile List<AlbumClickListener> listeners = new ArrayList<AlbumClickListener>();
    private IOnAlbumSelected userListener = null;
    public AlbumListPanel(List<Album> albumList) {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        albumPanel.setLayout(new GridLayout(0, 3));
        //Heavily rendering a lot of album covers, needs multithread
        Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
        albumList.forEach(album -> {
            executor.execute(()-> {
                if (album.getName().length() <= 0) {
                    System.out.println(album.getArtist());
                    album.getTracks().forEach(track -> {
                        System.out.println(track.getName() + track.getAlbumName() + track.getPath() + track.getDurationMs());
                    });
                }
                AlbumCard albumCard = new AlbumCard(album);
                AlbumClickListener albumClickListener = new AlbumClickListener(album);
                albumClickListener.setListener(this);
                albumPanel.add(albumCard);
                albumCard.addMouseListener(albumClickListener);
                listeners.add(albumClickListener);
            });
        });
        new Thread(()->{
            try {
                executor.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            albumPanel.invalidate();
            invalidate();
        });
        setViewportView(albumPanel);
        albumPanel.invalidate();
        getVerticalScrollBar().setUnitIncrement(18);
        invalidate();
    }

    public void setOnAlbumSelected(IOnAlbumSelected listener) {
        this.userListener = listener;
    }

    @Override
    public void onAlbumSelected(Album album) {
        if(userListener != null) {
            userListener.onAlbumSelected(album);
        }
    }
}
