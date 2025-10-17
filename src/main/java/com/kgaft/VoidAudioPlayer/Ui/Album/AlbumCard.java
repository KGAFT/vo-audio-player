package com.kgaft.VoidAudioPlayer.Ui.Album;

import com.kgaft.VoidAudioPlayer.Model.MAlbumUiObject;
import com.kgaft.VoidAudioPlayer.Ui.Util.ImageInflater;
import com.kgaft.VoidAudioPlayer.Verbose.Album;

import javax.swing.*;
import java.awt.*;

public class AlbumCard extends JPanel implements MAlbumUiObject {
    private Album album;
    private ImageIcon albumIcon;
    private JLabel coverLabel;
    private JPanel verbosePanel;
    private JLabel titleLabel;
    private JLabel artistLabel;
    public AlbumCard(Album album) {
        this.album = album;
        this.coverLabel = new JLabel();
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(150, 200));
        if(album.getCover() != null && album.getCover().length>0) {
            albumIcon = (ImageIcon) ImageInflater.loadImage(album.getCover(), 150, 150);
            coverLabel = new JLabel(albumIcon);
        }

        add(coverLabel, BorderLayout.CENTER);
        verbosePanel = new JPanel();
        verbosePanel.setLayout(new BoxLayout(verbosePanel, BoxLayout.Y_AXIS));
        titleLabel = new JLabel(album.getName());
        artistLabel = new JLabel(album.getArtist());
        verbosePanel.add(titleLabel);
        verbosePanel.add(artistLabel);
        add(verbosePanel, BorderLayout.SOUTH);
        invalidate();
    }

    @Override
    public String getAlbumName() {
        return album.getName();
    }

    @Override
    public Album getBaseAlbum() {
        return album;
    }
}
