package com.kgaft.VoidAudioPlayer;


import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.kgaft.VoidAudioPlayer.Model.MCollection;
import com.kgaft.VoidAudioPlayer.Model.MSettings;
import com.kgaft.VoidAudioPlayer.Ui.ProgressAcceptor;
import com.kgaft.VoidAudioPlayer.Ui.Swing.MainWindow;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.PlayerMenuWindow;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.PlayerTerminal;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.PlayerWindow;
import com.kgaft.VoidAudioPlayer.Verbose.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

//Testing purposes only, in future will be organised!

public class Main implements  ProgressAcceptor{
    static {
        File file = new File("target/release/");
        for (File listFile : file.listFiles()) {
            if (listFile.getName().endsWith(".so") || listFile.getName().endsWith(".dylib") || listFile.getName().endsWith(".dll")) {
                try {
                    System.load(listFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        MSettings.readSettingsOrDefault().applySettings();
        LibraryIndex index = LibraryIndex.getInstance();
        MCollection.init(index);
        //index.addDirectory("/mnt/hdd/Music", new Main());
        index.refreshCollection(new Main());
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }

        MainWindow window = new MainWindow();

        /*
        PlayerTerminal playerTerminal = new PlayerTerminal();
        PlayerMenuWindow menuWindow = new PlayerMenuWindow(playerTerminal);

        playerTerminal.start();
         */

    }

    @Override
    public void setProgress(float progress) {
        System.err.print("Current progress: "+progress);
    }

    @Override
    public void setCurrentState(String state) {
        System.err.println(" Current state: "+state);
    }
}
