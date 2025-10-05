package com.kgaft.VoidAudioPlayer.Ui.Util;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;

public class ImageResizer implements ComponentListener {
    private HashMap<String, JLabel> labels = new HashMap<>();
    private HashMap<byte[], JLabel> labelBytes = new HashMap<>();
    private float wScale = 1;
    private float hScale = 1;
    private ResizerWorkMode workMode;

    public ImageResizer(float wScale, float hScale, ResizerWorkMode workMode) {
        this.wScale = wScale;
        this.hScale = hScale;
        this.workMode = workMode;
    }

    public ImageResizer(ResizerWorkMode workMode) {
        this.workMode = workMode;
    }

    /// If button is dual state, push this button two times: first time - icon for first state, second time - icon for the second state
    public void pushLabel(String iconPath, JLabel label) {
        this.labels.put(iconPath, label);
    }
    public void pushLabelBytes(byte[] data, JLabel label) {
        this.labelBytes.put(data, label);
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
        labels.forEach((iconPath, button) -> {
            Icon icon = ImageInflater.loadImage(iconPath, targetExtent.width, targetExtent.height);
            button.setIcon(icon);
            button.setPreferredSize(new Dimension(targetExtent.width, targetExtent.height));
            button.repaint();
            button.invalidate();
            button.validate();
        });
        labelBytes.forEach((imageBytes, label) -> {
            Icon icon = ImageInflater.loadImage(imageBytes, targetExtent.width, targetExtent.height);
            label.setIcon(icon);
            label.setPreferredSize(new Dimension(targetExtent.width, targetExtent.height));
            label.repaint();
            label.invalidate();
            label.validate();
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
