package com.kgaft.VoidAudioPlayer.Ui.Util;

import javax.swing.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;


public class IconResizer implements ComponentListener {
    private HashMap<String, JButton> buttons = new HashMap<>();
    private float wScale = 1;
    private float hScale = 1;
    private ResizerWorkMode workMode;

    public IconResizer(float wScale, float hScale, ResizerWorkMode workMode) {
        this.wScale = wScale;
        this.hScale = hScale;
        this.workMode = workMode;
    }

    public IconResizer(ResizerWorkMode workMode) {
        this.workMode = workMode;
    }

    /// If button is dual state, push this button two times: first time - icon for first state, second time - icon for the second state
    public void pushButton(String iconPath, JButton button) {
        this.buttons.put(iconPath, button);
    }

    @Override
    public void componentResized(ComponentEvent e) {
        Extent targetExtent;
        switch (workMode) {
            case USE_ONLY_HEIGHT:
                targetExtent = new Extent((int) (e.getComponent().getHeight() * wScale), (int) (e.getComponent().getHeight() * hScale));
                break;
            case USE_ONLY_WIDTH:
                targetExtent = new Extent((int) (e.getComponent().getWidth() * wScale), (int) (e.getComponent().getWidth() * hScale));
                break;
            case USE_MINIMUM:
                int minimum = Math.min(e.getComponent().getWidth(), e.getComponent().getHeight());
                targetExtent = new Extent((int) (minimum * wScale), (int) (minimum * hScale));
                break;
            case USE_MAXIMUM:
                int maximum = Math.max(e.getComponent().getWidth(), e.getComponent().getHeight());
                targetExtent = new Extent((int) (maximum * wScale), (int) (maximum * hScale));
                break;
            default:
                targetExtent = new Extent((int) (e.getComponent().getWidth() * wScale), (int) (e.getComponent().getHeight() * hScale));
                break;
        }
        buttons.forEach((iconPath, button) -> {
            Icon icon = IconInflater.loadIcon(iconPath, targetExtent.width, targetExtent.height);
            button.setIcon(icon);
            button.repaint();
            button.invalidate();
        });
    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentShown(ComponentEvent e) {

    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }
}
