package com.kgaft.VoidAudioPlayer.Ui.Util;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.util.HashMap;


class VoIcon {
    FlatSVGIcon base;
    HashMap<Extent, Icon> iconsInstances = new HashMap<>();

    public VoIcon(FlatSVGIcon base) {
        this.base = base;
    }

    public Icon createIcon(Extent extent) {
        Icon icon = base.derive(extent.width, extent.height);
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

public class IconInflater {
    private static HashMap<String, VoIcon> iconIndex = new HashMap<>();

    public static Icon loadIcon(String path, int width, int height) {
        VoIcon icon = iconIndex.get(path);
        Extent extent = new Extent(width, height);
        if (icon != null) {
            return icon.createOrGetIcon(extent);
        }
        FlatSVGIcon flatSVGIcon = new FlatSVGIcon(path);
        VoIcon voIcon = new VoIcon(flatSVGIcon);
        iconIndex.put(path, voIcon);
        return voIcon.createIcon(extent);
    }
}
