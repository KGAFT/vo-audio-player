package com.kgaft.VoidAudioPlayer.Ui.Util;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class DualStateButton extends JButton implements ActionListener {
    private Icon firstIcon;
    private Icon secondIcon;
    private boolean state = false;
    private List<ActionListener> actionListener = new ArrayList<>();
    private byte setCall = 1;

    public DualStateButton(Icon firstIcon, Icon secondIcon) {
        super("play");
        this.firstIcon = firstIcon;
        this.secondIcon = secondIcon;
        /*


         */
        super.setIcon(firstIcon);
        setText("");
        super.addActionListener(this);
    }

    @Override
    public void addActionListener(ActionListener l){
        actionListener.add(l);
    }

    @Override
    public void setIcon(Icon icon) {
        if(setCall == 1){
            firstIcon = icon;
            setCall = 2;
        } else if(setCall == 2){
            secondIcon = icon;
            setCall = 1;
        }
        if(state) {
            super.setIcon(secondIcon);
        } else {
            super.setIcon(firstIcon);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(state) {
            state = false;
            super.setIcon(firstIcon);
        } else {
            state = true;
            super.setIcon(secondIcon);
        }
        actionListener.forEach(l -> l.actionPerformed(e));
    }

    public boolean isState() {
        return state;
    }
}
