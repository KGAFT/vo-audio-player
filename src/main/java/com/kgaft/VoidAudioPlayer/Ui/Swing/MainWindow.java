package com.kgaft.VoidAudioPlayer.Ui.Swing;

import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.*;
import com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.CollectionElement.CollectionElement;
import com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.PlayListElement.PlayListElement;
import com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.SettingsElement.SettingsElement;
import com.kgaft.VoidAudioPlayer.Ui.Swing.TrackControlPanel.PlayerPanel;

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
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout());

        rootContainer.add(new PlayerPanel(getWidth(), getHeight()), BorderLayout.SOUTH);
        rootContainer.add(new MenuSidePanel(elements, rootPanel), BorderLayout.WEST);
        rootContainer.add(rootPanel, BorderLayout.CENTER);
        add(rootContainer);
        setVisible(true);
        System.out.println(isOpaque());
    }
}
