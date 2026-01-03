package com.kgaft.VoidAudioPlayer.Ui.Swing;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private JPanel rootContainer = new JPanel();
    public MainWindow(){
        super("Void Audio Player");
        setSize(800, 600);
        rootContainer.setLayout(new BorderLayout());
        add(rootContainer);
    }
}
