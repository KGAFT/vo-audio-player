package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Panel;
import com.kgaft.VoidAudioPlayer.Model.MPlayer;
import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerWindow {
    public PlayerWindow(PlayerTerminal terminal) {
        BasicWindow window = new BasicWindow(StringManager.getString("playlist"));

        Panel panel = new Panel();
        ActionListBox playListBox = new ActionListBox();
        AtomicInteger indexCounter = new AtomicInteger();
        MPlayer.getPlayList().getTrackList().forEach(element -> {
            int finalIndex = indexCounter.getAndIncrement();
            playListBox.addItem(element.getName(), () -> {
                MPlayer.setPlayListPos(finalIndex);
            });
        });
        panel.addComponent(playListBox);
        panel.addComponent(new Button(StringManager.getString("play") + "/" + StringManager.getString("pause"), () -> {
            if (MPlayer.isPlaying()) {
                MPlayer.pause();
            } else {
                MPlayer.play();
            }
        }));
        panel.addComponent(new Button(StringManager.getString("next"), MPlayer::nextTrack));
        panel.addComponent(new Button(StringManager.getString("previous"), MPlayer::previousTrack));
        panel.addComponent(new Button(StringManager.getString("exit"), window::close));
        window.setComponent(panel);
        terminal.getGui().addWindow(window);
        terminal.registerWindow(window);
    }
}
