package com.kgaft.VoidAudioPlayer.Ui.Album;

import com.kgaft.VoidAudioPlayer.Model.MAlbumPlacingManager;
import com.kgaft.VoidAudioPlayer.Model.MAlbumUiObject;
import com.kgaft.VoidAudioPlayer.Model.MCollection;
import com.kgaft.VoidAudioPlayer.Verbose.Album;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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


public class AlbumListPanel extends JPanel implements IOnAlbumSelected {
    private JPanel albumPanel = new JPanel();
    private JScrollPane scrollPane;
    private volatile List<AlbumClickListener> listeners = new ArrayList<AlbumClickListener>();
    private IOnAlbumSelected userListener = null;
    private JTextField searchTextField;

    public AlbumListPanel(List<Album> albumList) {
        setLayout(new BorderLayout());
        searchTextField = new JTextField();
        searchTextField.setEditable(true);
        searchTextField.setToolTipText("Search albums");
        scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        albumPanel.setLayout(new GridLayout(0, 3));
        scrollPane.setViewportView(albumPanel);
        albumList.forEach(album -> {
            MAlbumPlacingManager.postAlbumAction(album, input ->{
                AlbumCard albumCard = new AlbumCard(album);
                AlbumClickListener albumClickListener = new AlbumClickListener(album);
                albumClickListener.setListener(this);
                albumCard.addMouseListener(albumClickListener);
                listeners.add(albumClickListener);
                return albumCard;
            });
        });
        MAlbumPlacingManager.sortAlbumsAlphabetically().forEach(album -> {
            albumPanel.add((AlbumCard)album);
        });

        albumPanel.invalidate();
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.invalidate();
        add(scrollPane, BorderLayout.CENTER);
        add(searchTextField, BorderLayout.NORTH);
        invalidate();
        searchTextField.setEnabled(true);
        searchTextField.enableInputMethods(true);
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void updateList(){
                List<MAlbumUiObject> results = searchTextField.getText().isEmpty() ? MAlbumPlacingManager.sortAlbumsAlphabetically() : MAlbumPlacingManager.searchAlbums(searchTextField.getText());
                albumPanel.removeAll();
                results.forEach(album -> {
                    albumPanel.add((AlbumCard)album);
                });
                albumPanel.invalidate();
                scrollPane.invalidate();
                scrollPane.validate();
                scrollPane.repaint();
                invalidate();
            }

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                updateList();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                updateList();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                updateList();
            }
        });
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
