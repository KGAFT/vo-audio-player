package com.kgaft.VoidAudioPlayer.Ui.Swing.MenuSIdePanel;

import javax.swing.*;
import java.awt.*;

public interface SidePanelElement {
    Dimension ICON_SIZE_PREFERRED = new Dimension(50, 50);
    Dimension WHOLE_SIZE_PREFERRED = new Dimension(75, 75);
    JPanel getEntryButton();
    JPanel contentPanel();
}
