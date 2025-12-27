package com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel;

import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.IOptionConsumer;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel.SelectorPanel;

public class TracksPanel extends SelectorPanel {
    /**
     * @param parentWindow
     * @param userData
     * @param panelToReturn         must be an object that extends panel and implements IOptionConsumer
     * @param panelToReturnOnOption
     * @param consumer
     * @param title
     * @param options
     */
    public TracksPanel(Window parentWindow, Object userData, Panel panelToReturn, Panel panelToReturnOnOption, IOptionConsumer consumer, Object[] options) {
        super(parentWindow, userData, panelToReturn, panelToReturnOnOption, consumer, "Select track", options);
    }

}
