package com.kgaft.VoidAudioPlayer.Ui.Terminal.CollectionPanel;

import com.googlecode.lanterna.gui2.ActionListBox;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Window;
import com.kgaft.VoidAudioPlayer.Model.MCollection;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.IOptionConsumer;
import com.kgaft.VoidAudioPlayer.Ui.Terminal.SelectorPanel.SelectorPanel;
import com.kgaft.VoidAudioPlayer.Verbose.Album;
import com.kgaft.VoidAudioPlayer.Verbose.Artist;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import java.util.List;

public class CollectionPanel extends Panel implements IOptionConsumer {
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
        exitButton = new Button("Back", () -> {
            parentWindow.setComponent(panelToReturn);
        });

        listBox.addItem("Albums --->", () -> {
            List<Album> albums = MCollection.getAlbums();
            AlbumPanel selectorPanel = new AlbumPanel(parentWindow, null, this, albums.toArray());
        });
        listBox.addItem("Tracks ---> ", () -> {
            List<Track> tracks = MCollection.getTracks();
            TracksPanel tracksPanel = new TracksPanel(parentWindow, null, this, null, this, tracks.toArray());
        });
        listBox.addItem("Artists ---> ", () -> {
            List<Artist> artists = MCollection.getArtists();
            ArtistsPanel artistsPanel = new ArtistsPanel(parentWindow, this, artists.toArray());
        });
        addComponent(listBox);
        addComponent(exitButton);
    }

    @Override
    public void optionSelected(long id, Object userData, Object option) {

    }
}
