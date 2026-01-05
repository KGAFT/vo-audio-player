package com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel.CollectionElement;

import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Verbose.LibraryIndex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

public class CollectionPanel extends JPanel implements ComponentListener {
    private JComboBox<String> displayModes;
    public CollectionPanel(){
        setLayout(new BorderLayout());
        String[] modesItems = new String[]{StringManager.getString("albums"), StringManager.getString("artists"), StringManager.getString("tracks")};
        displayModes = new JComboBox<>(modesItems);
        add(displayModes, BorderLayout.NORTH);
    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {
        int width = getWidth();
        displayModes.setPreferredSize(new Dimension(width, 40));
        displayModes.repaint();
        displayModes.invalidate();
        invalidate();
    }

    @Override
    public void componentMoved(ComponentEvent componentEvent) {

    }

    @Override
    public void componentShown(ComponentEvent componentEvent) {

    }

    @Override
    public void componentHidden(ComponentEvent componentEvent) {

    }
}
