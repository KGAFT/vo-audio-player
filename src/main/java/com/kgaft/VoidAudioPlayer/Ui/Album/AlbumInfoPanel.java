package com.kgaft.VoidAudioPlayer.Ui.Album;

import com.kgaft.VoidAudioPlayer.Native.Track;
import com.kgaft.VoidAudioPlayer.Ui.TrackEntry;
import com.kgaft.VoidAudioPlayer.Ui.Util.*;
import com.kgaft.VoidAudioPlayer.Verbose.Album;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

class PActionListener implements ActionListener {
    private Track track;
    private AlbumInfoPanel infoPanel;

    public PActionListener(Track track, AlbumInfoPanel infoPanel) {
        this.track = track;
        this.infoPanel = infoPanel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        infoPanel.onTrackSelected.onTrackSelected(track);
    }
}

public class AlbumInfoPanel extends JPanel implements ComponentListener {
    private JLabel albumCover;
    private JLabel albumNameLabel;
    private JLabel albumArtistLabel;
    private JLabel albumYearLabel;
    private JLabel genreLabel;

    private JPanel albumInfoPanel;
    private JPanel songsPanel;
    private JPanel albumTextInfoPanel;
    private JScrollPane songsScroll;
    private JButton backButton;
    protected IOnTrackSelected onTrackSelected;
    private IconResizer iconResizer = new IconResizer(0.05f, 0.05f, ResizerWorkMode.USE_MINIMUM);
    private ImageResizer imageResizer = new ImageResizer(0.6f, 0.6f, ResizerWorkMode.USE_MINIMUM);
    public AlbumInfoPanel(Album album) {
        albumCover = new JLabel();
        albumNameLabel = new JLabel(album.getName());
        albumArtistLabel = new JLabel(album.getArtist());
        albumYearLabel = new JLabel(""+album.getYear());
        genreLabel = new JLabel(album.getGenre());
        
        
        backButton = new JButton(IconInflater.loadIcon("icons/back.svg", 10, 10));
        iconResizer.pushButton("icons/back.svg", backButton);


        songsScroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        if(album.getCover()!=null && album.getCover().length !=0) {
            Icon scaled = ImageInflater.loadImage(album.getCover(), 150, 150);
            albumCover.setIcon(scaled);
            imageResizer.pushLabelBytes(album.getCover(), albumCover);

            albumCover.setPreferredSize(new Dimension(scaled.getIconWidth(), scaled.getIconHeight()));
            addComponentListener(imageResizer);
        }
        addComponentListener(iconResizer);
        addComponentListener(this);

        albumInfoPanel = new JPanel();
        albumInfoPanel.setLayout(new BorderLayout());
        albumTextInfoPanel = new JPanel();
        albumTextInfoPanel.setLayout(new GridLayout(0, 1));


        albumTextInfoPanel.add(albumNameLabel);
        albumTextInfoPanel.add(albumArtistLabel);
        albumTextInfoPanel.add(albumYearLabel);
        albumTextInfoPanel.add(genreLabel);

        albumInfoPanel.add(albumCover, BorderLayout.CENTER);
        albumInfoPanel.add(albumTextInfoPanel, BorderLayout.SOUTH);
        albumInfoPanel.setPreferredSize(new Dimension(100, 200));

        songsPanel = new JPanel();
        songsPanel.setLayout(new GridLayout(0, 1));

        songsScroll.setViewportView(songsPanel);
        songsScroll.getVerticalScrollBar().setUnitIncrement(18);

        setLayout(new BorderLayout());
        add(albumInfoPanel, BorderLayout.NORTH);
        add(songsScroll, BorderLayout.CENTER);
        add(backButton, BorderLayout.WEST);

        int counter = 1;
        for (Track track : album.getTracks()) {
            TrackEntry entry = new TrackEntry(track.getName(), counter, (int)(track.getDurationMs()), false);
            entry.addOnPlayButtonListener(new PActionListener(track, this));
            songsPanel.add(entry);
            counter++;
        }
    }

    public void setOnBackButtonPressed(ActionListener actionListener) {
        backButton.addActionListener(actionListener);
    }

    public void onTrackSelected(IOnTrackSelected onTrackSelected) {
        this.onTrackSelected = onTrackSelected;
    }

    @Override
    public void componentResized(ComponentEvent e) {
        albumInfoPanel.setPreferredSize(new Dimension(getWidth()/8, getHeight()/3));
        albumInfoPanel.repaint();
        albumInfoPanel.revalidate();
        albumInfoPanel.validate();
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }
}
