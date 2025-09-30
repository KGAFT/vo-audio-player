package com.kgaft.VoidAudioPlayer.Ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class TrackEntry extends JPanel implements MouseListener {

    private JLabel entryNumber;
    private JLabel trackNameLabel;
    private JLabel trackDurationLabel;
    private JPanel leftSidePanel;
    private JPanel rightSidePanel;
    private JButton deleteButton;
    private ActionListener actionListener = null;
    private Color oldBackground;

    public static String formatTime(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public TrackEntry(String trackName, int trackNumber, int trackDurationMs, boolean deleteButton) {
        if(trackNumber!=-1){
            entryNumber = new JLabel("" + trackNumber);
        } else {
            entryNumber = new JLabel();
        }

        if(deleteButton){
            this.deleteButton = new JButton("delete");
        }

        trackNameLabel = new JLabel(trackName);
        trackDurationLabel = new JLabel(formatTime(trackDurationMs));
        leftSidePanel = new JPanel();
        rightSidePanel = new JPanel();

        leftSidePanel.setLayout(new FlowLayout());
        rightSidePanel.setLayout(new FlowLayout());

        rightSidePanel.add(trackDurationLabel);

        leftSidePanel.add(entryNumber);
        leftSidePanel.add(trackNameLabel);

        addMouseListener(this);
        setLayout(new BorderLayout());
        add(leftSidePanel, BorderLayout.WEST);
        add(rightSidePanel, BorderLayout.EAST);
    }

    public void addOnPlayButtonListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(actionListener != null) {
            actionListener.actionPerformed(null);
        }

    }

    @Override
    public void mousePressed(MouseEvent e) {
        oldBackground = getBackground();
        setBackground(Color.GRAY);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        setBackground(oldBackground);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

}
