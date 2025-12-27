package com.kgaft.VoidAudioPlayer;


import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.kgaft.VoidAudioPlayer.Model.MCollection;
import com.kgaft.VoidAudioPlayer.Model.MSettings;
import com.kgaft.VoidAudioPlayer.Ui.ProgressAcceptor;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.PlayerTerminal;
import com.kgaft.VoidAudioPlayer.Verbose.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

//Testing purposes only, in future will be organised!

public class Main implements  ProgressAcceptor{
    static {
        File file = new File("target/debug");
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
  //      index.addDirectory("/mnt/hdd/Music", new Main());
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }


        /*
        List<DsdDeviceInfo> devices = MPlayer.enumerateDsdDevices();
        for (int i = 0; i < devices.size(); i++) {
            System.out.println("id: "+i +" device name: "+devices.get(i).getName());
        }
        System.out.println("Select dsd device: ");
        Scanner scanner = new Scanner(System.in);
        DsdDeviceInfo name = devices.get(scanner.nextInt());
        MPlayer.initDsdDevice(name);

         */
        PlayerTerminal playerTerminal = new PlayerTerminal();

    }

    @Override
    public void setProgress(float progress) {
        System.out.print("Current progress: "+progress);
    }

    @Override
    public void setCurrentState(String state) {
        System.out.println(" Current state: "+state);
    }
}
