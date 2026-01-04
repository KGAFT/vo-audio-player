package com.kgaft.VoidAudioPlayer.Ui.Swing;

import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class MainWindow extends JFrame {
    private JPanel rootContainer = new JPanel();
    public MainWindow(){
        super(StringManager.getString("app_name"));
        setSize(800, 600);
        rootContainer.setLayout(new BorderLayout());
        ArrayList<SidePanelElement> elements = new ArrayList<>();
        elements.add(new PlayListElement());
        elements.add(new CollectionElement());
        elements.add(new SettingsElement());
        rootContainer.add(new MenuSidePanel(elements));

        add(rootContainer);
        setVisible(true);
    }
}
