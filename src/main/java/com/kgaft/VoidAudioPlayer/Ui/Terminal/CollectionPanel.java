package com.kgaft.VoidAudioPlayer.Ui.Terminal;

import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;

public class CollectionPanel extends Panel {
    private Window parentWindow;
    private Panel panelToReturn;

    public CollectionPanel(Window parentWindow, Panel panelToReturn){
        this.parentWindow = parentWindow;
        this.panelToReturn = panelToReturn;
    }
}
