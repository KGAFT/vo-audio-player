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
    private JPanel contentPanel;
    private SidePanelElement currentElement;
    public MenuSidePanel(List<SidePanelElement> elements, JPanel contentPanel){
        this.elements = elements;
        this.currentElement = this.elements.getFirst();
        this.contentPanel = contentPanel;
        setLayout(new GridLayout(1, 1));
        entriesPanel.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                System.out.println("clicked");
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
        add(entriesPanel);
        makeEntriesPanel();
        makeContentPanel();
        invalidate();
    }

    private void makeEntriesPanel(){
        entriesPanel.removeAll();

        entriesPanel.setLayout(new BoxLayout(entriesPanel, BoxLayout.Y_AXIS));
        elements.forEach(element->{
            JPanel button = element.getEntryButton();
            button.addMouseListener(new MouseListener() {
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
            entriesPanel.add(button);
        });
        entriesPanel.add(Box.createVerticalGlue());
        entriesPanel.invalidate();
    }

    private void makeContentPanel(){
        contentPanel.removeAll();
        contentPanel.add(currentElement.contentPanel());
        contentPanel.repaint();
        contentPanel.revalidate();
        contentPanel.invalidate();

    }


}
