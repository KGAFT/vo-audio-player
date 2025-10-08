package com.kgaft.VoidAudioPlayer.Ui.Util;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

class VoImage {
    BufferedImage base;
    HashMap<Extent, Icon> iconsInstances = new HashMap<>();

    public VoImage(BufferedImage base) {
        this.base = base;
    }

    public Icon createIcon(Extent extent) {
        Icon icon = new ImageIcon(base.getScaledInstance(extent.width, extent.height, Image.SCALE_SMOOTH));
        iconsInstances.put(extent, icon);
        return icon;
    }

    public Icon createOrGetIcon(Extent extent) {
        Icon icon = iconsInstances.get(extent);
        if (icon == null) {
            icon = createIcon(extent);
            iconsInstances.put(extent, icon);
            return icon;
        }
        cleanupIcons();
        return icon;
    }

    public void cleanupIcons() {
        while (iconsInstances.size() > 5) {
            iconsInstances.remove(iconsInstances.keySet().iterator().next());
        }
    }
}

public class ImageInflater {

    private static HashMap<String, VoImage> imageIndex = new HashMap<>();


    public static Icon loadImage(String path, int width, int height) {
        VoImage icon = imageIndex.get(path);
        Extent extent = new Extent(width, height);
        if (icon != null) {
            return icon.createOrGetIcon(extent);
        }
        BufferedImage img = null;
        try {
            img = ImageIO.read(new FileInputStream(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        VoImage voIcon = new VoImage(img);
        imageIndex.put(path, voIcon);
        return voIcon.createIcon(extent);
    }

    public static Icon loadImage(byte[] data, int width, int height) {
        String hashCode = String.valueOf(Arrays.hashCode(data));
        VoImage icon = imageIndex.get(hashCode);
        Extent extent = new Extent(width, height);
        if (icon != null) {
            return icon.createOrGetIcon(extent);
        }
        BufferedImage img = null;
        try {
            img = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        VoImage voIcon = new VoImage(img);
        imageIndex.put(hashCode, voIcon);
        return voIcon.createIcon(extent);
    }

}
