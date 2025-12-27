package com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel;

import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.IOptionConsumer;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel.SelectorPanel;
import com.kgaft.VoidAudioPlayer.Verbose.Artist;

public class ArtistsPanel implements IOptionConsumer {
    private Window parentWindow;
    private SelectorPanel selectorPanel;
    public ArtistsPanel(Window parentWindow, Panel panelToReturn, Object[] artists){
        this.parentWindow= parentWindow;
        this.selectorPanel = new SelectorPanel(parentWindow, null, panelToReturn, null, this, "Select artist", artists);
    }

    @Override
    public void optionSelected(long id, Object userData, Object option) {
        Artist artist = (Artist) option;
        AlbumPanel albumPanel = new AlbumPanel(parentWindow, null, selectorPanel, artist.getAlbums().toArray());
    }
}
