package com.kgaft.VoidAudioPlayer.Ui;

import com.kgaft.VoidAudioPlayer.Model.MPlayer;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PlaylistPanel extends JScrollPane{
    private List<TrackEntry> entryList = new ArrayList<>();
    private JPanel trackPanel = new JPanel();

    public PlaylistPanel() {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        trackPanel.setLayout(new BoxLayout(trackPanel, BoxLayout.Y_AXIS));
        getVerticalScrollBar().setUnitIncrement(18);
        setViewportView(trackPanel);
        trackPanel.invalidate();
        updateTracks();
    }

    public void addTrack(Track track, int trackNumber) {
        TrackEntry entry = new TrackEntry(track.getName(), trackNumber, (int) track.getDurationMs(), false);
        entry.addOnPlayButtonListener(e -> {
            MPlayer.startPlayingTrackFromPlaylist(trackNumber);
        });
        entry.setMaximumSize(new Dimension(800000, 40));
        entryList.add(entry);
        trackPanel.add(entry);
        trackPanel.revalidate();
        trackPanel.repaint();
        invalidate();
        repaint();
    }

    public void updateTracks(){
        entryList.clear();
        trackPanel.removeAll();
        trackPanel.revalidate();
        AtomicInteger counter = new AtomicInteger();
        MPlayer.getPlayList().getTrackList().forEach(element->addTrack(element, counter.getAndIncrement()));
    }


}
