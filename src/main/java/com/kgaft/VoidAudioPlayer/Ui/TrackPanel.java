package com.kgaft.VoidAudioPlayer.Ui;

import com.kgaft.VoidAudioPlayer.Native.Track;
import com.kgaft.VoidAudioPlayer.Ui.Util.ImageInflater;
import com.kgaft.VoidAudioPlayer.Ui.Util.ImageResizer;
import com.kgaft.VoidAudioPlayer.Ui.Util.ResizerWorkMode;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

public class TrackPanel extends JPanel {
    private JLabel coverLabel;
    private JLabel nameLabel;
    private JLabel artistLabel;
    private JLabel albumLabel;
    private JLabel genreLabel;
    private JLabel yearLabel;
    private JLabel pathLabel;
    private JLabel bitDepthLabel;
    private JLabel sampleRateLabel;
    private JLabel channelsLabel;
    private JLabel bitrateLabel;
    private ImageResizer imageResize = new ImageResizer(0.8f, 0.8f, ResizerWorkMode.USE_MINIMUM);
    public TrackPanel(Track track) {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Cover art
        coverLabel = new JLabel();
        coverLabel.setPreferredSize(new Dimension(250, 250));
        if (track.getPictureBytes() != null) {
            try {

                coverLabel.setIcon(ImageInflater.loadImage(track.getPictureBytes(), 250, 250));
                imageResize.pushLabelBytes(track.getPictureBytes(), coverLabel);
            } catch (Exception ignored) {
            }
        }
        add(coverLabel, BorderLayout.NORTH);
        addComponentListener(imageResize);
        // Info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(0, 1, 5, 5));

        nameLabel = new JLabel("Title: " + safe(track.getName()));
        artistLabel = new JLabel("Artist: " + safe(track.getArtistName()));
        albumLabel = new JLabel("Album: " + safe(track.getAlbumName()));
        genreLabel = new JLabel("Genre: " + safe(track.getGenre()));
        yearLabel = new JLabel("Year: " + (track.getYear() != 0 ? track.getYear() : "Unknown"));
        pathLabel = new JLabel("Path: " + safe(track.getPath()));
        bitDepthLabel = new JLabel("Bit Depth: " + (track.getBitDepth() > 0 ? track.getBitDepth() + " bit" : "Unknown"));
        sampleRateLabel = new JLabel("Sample Rate: " + (track.getSampleRate() > 0 ? track.getSampleRate() + " Hz" : "Unknown"));
        channelsLabel = new JLabel("Channels: " + (track.getChannels() > 0 ? track.getChannels() : "Unknown"));
        bitrateLabel = new JLabel("Bitrate: " + (track.getOverallBitrate() > 0 ? track.getOverallBitrate() + " kbps" : "Unknown"));

        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        artistLabel.setFont(artistLabel.getFont().deriveFont(Font.BOLD));
        albumLabel.setFont(albumLabel.getFont().deriveFont(Font.BOLD));

        infoPanel.add(nameLabel);
        infoPanel.add(artistLabel);
        infoPanel.add(albumLabel);
        infoPanel.add(genreLabel);
        infoPanel.add(yearLabel);
        infoPanel.add(pathLabel);
        infoPanel.add(bitDepthLabel);
        infoPanel.add(sampleRateLabel);
        infoPanel.add(channelsLabel);
        infoPanel.add(bitrateLabel);

        add(infoPanel, BorderLayout.CENTER);
    }

    private String safe(String val) {
        return val == null ? "Unknown" : val;
    }
}