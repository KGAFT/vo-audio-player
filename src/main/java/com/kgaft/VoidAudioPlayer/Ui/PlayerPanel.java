package com.kgaft.VoidAudioPlayer.Ui;

import com.kgaft.VoidAudioPlayer.Model.MPlayer;
import com.kgaft.VoidAudioPlayer.Native.Player;
import com.kgaft.VoidAudioPlayer.Ui.Util.DualStateButton;
import com.kgaft.VoidAudioPlayer.Ui.Util.IconInflater;
import com.kgaft.VoidAudioPlayer.Ui.Util.IconResizer;
import com.kgaft.VoidAudioPlayer.Ui.Util.ResizerWorkMode;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class PlayerPanel extends JPanel{
    private DualStateButton playPauseButton;
    private JButton prevTrackButton;
    private JButton nextTrackButton;
    private TrackControlFlow controlFlow;
    private JPanel manipulationsPanel;
    private IconResizer resizer;
    public PlayerPanel(int width, int height) {
        super();
        resizer = new IconResizer(0.7f,0.7f, ResizerWorkMode.USE_MINIMUM);

        addComponentListener(resizer);
        int size = Math.min(width, height);
        manipulationsPanel = new JPanel();
        manipulationsPanel.setLayout(new GridLayout(1, 3));


        java.util.List<String> devices = Player.getDevices();
        devices.forEach(System.out::println);

        Icon playIcon = IconInflater.loadIcon("icons/play.svg", size, size);
        Icon pauseIcon = IconInflater.loadIcon("icons/pause.svg", size, size);
        manipulationsPanel.setSize(width, height);
        playPauseButton = new DualStateButton(pauseIcon, playIcon);
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
        controlFlow = new TrackControlFlow();

        add(manipulationsPanel, BorderLayout.WEST);
        add(controlFlow,  BorderLayout.CENTER);
        playPauseButton.addActionListener(e -> {
            if (!playPauseButton.isState()) {
                MPlayer.play();
                controlFlow.startControlFlow();
            } else {
                MPlayer.pause();
                controlFlow.stopControlFlow();
            }
        });
    }


    public void addOnNextTrackButtonListener(ActionListener actionListener) {
        nextTrackButton.addActionListener(actionListener);
    }

    public void addOnPrevTrackButtonListener(ActionListener actionListener) {
        prevTrackButton.addActionListener(actionListener);
    }

    public void loadTrack(Track track) {
        MPlayer.getPlayList().pushTrack(track);
        MPlayer.startPlaying(0);
        controlFlow.startControlFlow();
    }

}
