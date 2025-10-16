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
import java.util.concurrent.atomic.AtomicInteger;

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
        setViewportView(albumPanel);
        //Heavily rendering a lot of album covers, needs multithread
        Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()-1);
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger decCounter = new AtomicInteger(albumList.size());
        List<AlbumCard> cardsPrepared = new ArrayList<>(albumList.size());
        albumList.forEach(album -> {
            int indexFinal = counter.get();
            cardsPrepared.add(null);
            executor.execute(()-> {
                AlbumCard albumCard = new AlbumCard(album);
                AlbumClickListener albumClickListener = new AlbumClickListener(album);
                albumClickListener.setListener(this);
                cardsPrepared.add(indexFinal, albumCard);
                albumCard.addMouseListener(albumClickListener);
                listeners.add(albumClickListener);
                decCounter.decrementAndGet();
            });
            counter.incrementAndGet();
        });
        while(decCounter.get()>0);
        cardsPrepared.forEach(albumCard -> {
            if(albumCard!=null) {
                albumPanel.add(albumCard);
            }
        });
        albumPanel.invalidate();
        invalidate();

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
