package com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.kgaft.VoidAudioPlayer.Model.MCollection;
import com.kgaft.VoidAudioPlayer.Ui.StringUtils.StringManager;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.IOptionConsumer;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel.SelectorPanel;
import com.kgaft.VoidAudioPlayer.Verbose.Album;
import com.kgaft.VoidAudioPlayer.Verbose.Artist;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import java.util.List;

public class CollectionPanel extends Panel {
    private Window parentWindow;
    private Panel panelToReturn;
    private ActionListBox listBox;
    private Button exitButton;

    public CollectionPanel(Window parentWindow, Panel panelToReturn) {
        this.parentWindow = parentWindow;
        this.panelToReturn = panelToReturn;
        refreshPage();
    }

    private void refreshPage() {
        removeAllComponents();
        listBox = new ActionListBox();
        exitButton = new Button(StringManager.getString("exit"), () -> {
            parentWindow.setComponent(panelToReturn);
        });

        listBox.addItem(StringManager.getString("albums"), () -> {
            List<Album> albums = MCollection.getAlbums();
            AlbumPanel selectorPanel = new AlbumPanel(parentWindow, null, this, albums.toArray());
        });
        listBox.addItem(StringManager.getString("tracks"), () -> {
            List<Track> tracks = MCollection.getTracks();
            TracksPanel tracksPanel = new TracksPanel(parentWindow, null, this, tracks.toArray());
        });
        listBox.addItem(StringManager.getString("artists"), () -> {
            List<Artist> artists = MCollection.getArtists();
            ArtistsPanel artistsPanel = new ArtistsPanel(parentWindow, this, artists.toArray());
        });
        addComponent(listBox);
        addComponent(exitButton);
    }


}
