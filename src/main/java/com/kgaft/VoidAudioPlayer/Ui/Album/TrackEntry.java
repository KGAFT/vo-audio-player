package com.kgaft.VoidAudioPlayer.Ui.Album;

import com.kgaft.VoidAudioPlayer.Ui.Util.DualStateButton;
import com.kgaft.VoidAudioPlayer.Ui.Util.IconInflater;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class TrackEntry extends JPanel {
    private JLabel entryNumber;
    private JLabel trackNameLabel;
    private JLabel trackDurationLabel;
    private DualStateButton playButton;

    private JPanel leftSidePanel;
    private JPanel rightSidePanel;

    public TrackEntry(String trackName, int trackNumber, int trackDuration) {
        entryNumber = new JLabel("" + trackNumber);
        trackNameLabel = new JLabel(trackName);
        trackDurationLabel = new JLabel("" + trackDuration);
        leftSidePanel = new JPanel();
        rightSidePanel = new JPanel();
        playButton = new DualStateButton(IconInflater.loadIcon("icons/play.svg", 10, 10), IconInflater.loadIcon("icons/pause.svg", 10, 10));

        leftSidePanel.setLayout(new FlowLayout());
        rightSidePanel.setLayout(new FlowLayout());

        leftSidePanel.add(entryNumber);
        leftSidePanel.add(trackNameLabel);

        rightSidePanel.add(playButton);

        setLayout(new BorderLayout());
        add(leftSidePanel, BorderLayout.WEST);
        add(rightSidePanel, BorderLayout.EAST);
    }

    public void addOnPlayButtonListener(ActionListener actionListener) {
        playButton.addActionListener(actionListener);
    }
}
