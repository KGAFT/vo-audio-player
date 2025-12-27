package com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel;

import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.IOptionConsumer;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel.SelectorPanel;
import com.kgaft.VoidAudioPlayer.Verbose.Album;

public class AlbumPanel implements IOptionConsumer {
    private Window parentWindow;
    private Panel panelToReturn;
    private SelectorPanel selectorPanel;
    /**
     * @param parentWindow
     * @param userData
     * @param panelToReturn must be an object that extends panel and implements IOptionConsumer
     * @param options
     */
    public AlbumPanel(Window parentWindow, Object userData, Panel panelToReturn, Object[] options) {
        this.panelToReturn = panelToReturn;
        this.parentWindow = parentWindow;
        selectorPanel = new SelectorPanel(parentWindow, userData, panelToReturn,null, this, "Select album", options);
    }

    @Override
    public void optionSelected(long id, Object userData, Object option) {
        Album album = (Album) option;
        TracksPanel tracksPanel = new TracksPanel(parentWindow, null, selectorPanel, null, this, album.getTracks().toArray());
    }
}
