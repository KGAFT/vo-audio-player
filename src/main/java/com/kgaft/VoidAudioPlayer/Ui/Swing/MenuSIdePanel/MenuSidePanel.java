package com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class MenuSidePanel extends JPanel {
    private List<SidePanelElement> elements = new ArrayList<>();
    private JPanel entriesPanel = new JPanel();
    private JPanel contentPanel = new JPanel();
    private SidePanelElement currentElement;
    public MenuSidePanel(List<SidePanelElement> elements){
        this.elements = elements;
        this.currentElement = this.elements.getFirst();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(entriesPanel);
        add(contentPanel);
        makeEntriesPanel();
        makeContentPanel();

    }

    private void makeEntriesPanel(){
        entriesPanel.removeAll();
        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        elements.forEach(element->{
            element.getEntryButton().addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    currentElement = element;
                    makeContentPanel();
                }

                @Override
                public void mousePressed(MouseEvent mouseEvent) {

                }

                @Override
                public void mouseReleased(MouseEvent mouseEvent) {

                }

                @Override
                public void mouseEntered(MouseEvent mouseEvent) {

                }

                @Override
                public void mouseExited(MouseEvent mouseEvent) {

                }
            });
            entriesPanel.add(element.getEntryButton());
        });
        entriesPanel.add(Box.createVerticalGlue());
    }

    private void makeContentPanel(){
        contentPanel.removeAll();
        contentPanel.add(currentElement.contentPanel());
    }


}
