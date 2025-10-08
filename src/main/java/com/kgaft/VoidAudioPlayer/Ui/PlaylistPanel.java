package com.kgaft.VoidAudioPlayer.Ui;

import com.kgaft.VoidAudioPlayer.Verbose.Track;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PlaylistPanel extends JScrollPane {
    private List<TrackEntry> entryList = new ArrayList<>();
    private List<Track> trackList = new ArrayList<>();
    private JPanel trackPanel = new JPanel();

    public PlaylistPanel() {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        trackPanel.setLayout(new BoxLayout(trackPanel, BoxLayout.Y_AXIS));
        getVerticalScrollBar().setUnitIncrement(18);
        setViewportView(trackPanel);
        trackPanel.invalidate();
    }

    public void addTrack(Track track) {
        trackList.add(track);
        TrackEntry entry = new TrackEntry(track.getName(), -1, (int) track.getDurationMs(), false);
        entryList.add(entry);
        trackPanel.add(entry);
        trackPanel.revalidate();
        trackPanel.repaint();
        invalidate();
        repaint();
    }
}
