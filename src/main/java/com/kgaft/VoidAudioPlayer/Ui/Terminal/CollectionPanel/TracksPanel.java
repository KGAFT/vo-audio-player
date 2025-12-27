package com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel;

import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.kgaft.VoidAudioPlayer.Model.MPlayer;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.IOptionConsumer;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel.SelectorPanel;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

public class TracksPanel  implements IOptionConsumer{
    private SelectorPanel selectorPanel;
    /**
     * @param parentWindow
     * @param userData
     * @param panelToReturn         must be an object that extends panel and implements IOptionConsumer

     * @param options
     */
    public TracksPanel(Window parentWindow, Object userData, Panel panelToReturn, Object[] options) {
        selectorPanel = new SelectorPanel(parentWindow, userData, panelToReturn, null, this, "Select track", options);
    }

    @Override
    public void optionSelected(long id, Object userData, Object option) {
        MPlayer.getPlayList().pushTrack((Track)option);
    }
}
