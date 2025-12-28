package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Panel;
import com.kgaft.VoidAudioPlayer.Model.MPlayer;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerWindow {
    public PlayerWindow(PlayerTerminal terminal){
        BasicWindow window = new BasicWindow("playlist");

        Panel panel = new Panel();
        ActionListBox playListBox = new ActionListBox();
        AtomicInteger indexCounter = new AtomicInteger();
        MPlayer.getPlayList().getTrackList().forEach(element -> {
            int finalIndex = indexCounter.getAndIncrement();
            playListBox.addItem(element.getName(), () -> {
               MPlayer.startPlayingTrackFromPlaylist(finalIndex);
            });
        });
        panel.addComponent(playListBox);
        panel.addComponent(new Button("Play/Pause", () -> {
            if (MPlayer.isPlaying()) {
                MPlayer.pause();
            } else {
                MPlayer.play();
            }
        }));
        panel.addComponent(new Button("Next", MPlayer::nextTrack));
        panel.addComponent(new Button("Previous", MPlayer::previousTrack));
        panel.addComponent(new Button("Exit", window::close));
        window.setComponent(panel);
        terminal.getGui().addWindow(window);
        terminal.registerWindow(window);
    }
}
