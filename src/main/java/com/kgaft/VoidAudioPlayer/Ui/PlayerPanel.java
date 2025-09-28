package com.kgaft.VoidAudioPlayer.Ui;

import com.kgaft.VoidAudioPlayer.Native.Player;
import com.kgaft.VoidAudioPlayer.Ui.Util.DualStateButton;
import com.kgaft.VoidAudioPlayer.Ui.Util.IconInflater;
import com.kgaft.VoidAudioPlayer.Ui.Util.IconResizer;
import com.kgaft.VoidAudioPlayer.Ui.Util.ResizerWorkMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class PlayerPanel extends JPanel{
    private DualStateButton playPauseButton;
    private JButton prevTrackButton;
    private JButton nextTrackButton;
    private TrackControlFlow controlFlow;
    private long playerHandle;
    private JPanel manipulationsPanel;
    private IconResizer resizer;
    private boolean firstTime = true;
    public PlayerPanel(int width, int height) {
        super();
        resizer = new IconResizer(0.7f,0.7f, ResizerWorkMode.USE_MINIMUM);

        addComponentListener(resizer);
        int size = Math.min(width, height);
        manipulationsPanel = new JPanel();
        manipulationsPanel.setLayout(new GridLayout(1, 3));
        playerHandle = Player.initializePlayer();

        Icon playIcon = IconInflater.loadIcon("icons/play.svg", size, size);
        Icon pauseIcon = IconInflater.loadIcon("icons/pause.svg", size, size);
        manipulationsPanel.setSize(width, height);
        playPauseButton = new DualStateButton(playIcon, pauseIcon);
        prevTrackButton = new JButton(IconInflater.loadIcon("icons/previous.svg", size,size));
        nextTrackButton = new JButton(IconInflater.loadIcon("icons/next.svg", size, size));

        resizer.pushButton("icons/play.svg", playPauseButton);
        resizer.pushButton("icons/pause.svg", playPauseButton);
        resizer.pushButton("icons/previous.svg", prevTrackButton);
        resizer.pushButton("icons/next.svg", nextTrackButton);

        manipulationsPanel.add(prevTrackButton);
        manipulationsPanel.add(playPauseButton);
        manipulationsPanel.add(nextTrackButton);

        setLayout(new BorderLayout());
        controlFlow = new TrackControlFlow(playerHandle);

        add(manipulationsPanel, BorderLayout.WEST);
        add(controlFlow,  BorderLayout.CENTER);
        playPauseButton.addActionListener(e -> {
            if (!playPauseButton.isState()) {
                Player.setPlaying(playerHandle, true);
                controlFlow.startControlFlow();
            } else {
                Player.setPlaying(playerHandle, false);
                controlFlow.stopControlFlow();
            }
        });
    }

    public void seekTrack(long offset) {
        Player.seekTrack(playerHandle, offset);
    }


    public void addOnNextTrackButtonListener(ActionListener actionListener) {
        nextTrackButton.addActionListener(actionListener);
    }

    public void addOnPrevTrackButtonListener(ActionListener actionListener) {
        prevTrackButton.addActionListener(actionListener);
    }

    public void loadTrack(String path) {
        path = path.replace("\\", "/");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if(!firstTime) {
            Player.setPlaying(playerHandle, true);
            controlFlow.stopControlFlow();
        }
        firstTime = false;

        Player.stop(playerHandle);
        Player.loadTrack(playerHandle, path);
    }

}
