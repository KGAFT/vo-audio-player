package com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel;

import javax.swing.*;
import java.awt.*;

public interface SidePanelElement {
    public static final Dimension ICON_SIZE_PREFERRED = new Dimension(50, 50);
    public static final Dimension WHOLE_SIZE_PREFERRED = new Dimension(75, 75);
    JPanel getEntryButton();
    JPanel contentPanel();
}
