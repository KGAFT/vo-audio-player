package com.kgaft.VoidAudioPlayer.Model;

import com.kgaft.VoidAudioPlayer.Ui.Album.AlbumCard;
import com.kgaft.VoidAudioPlayer.Verbose.Album;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import java.util.*;
import java.util.stream.Collectors;

class AlbumAction{
    public Album album;
    public MAlbumInitializeAction action;
}

public class MAlbumPlacingManager {
    private static volatile List<MAlbumUiObject> initializedAlbums = new ArrayList<>();
    private static volatile List<AlbumAction> pendingAlbums = new ArrayList<>();
    private static List<MAlbumUiObject> sortedAlbums = new ArrayList<>();

    public static List<MAlbumUiObject> sortAlbumsAlphabetically() {
        processPendingAlbums();
        if(sortedAlbums.size() != initializedAlbums.size()){
            sortedAlbums = initializedAlbums.parallelStream().sorted(Comparator.comparing(MAlbumUiObject::getAlbumName)).collect(Collectors.toCollection(ArrayList::new));
        }
        return sortedAlbums;
    }

    public static List<MAlbumUiObject> searchAlbums(String name) {
        processPendingAlbums();
        String finalName = name.toLowerCase();
        return initializedAlbums.parallelStream().filter(album -> {
            if(album.getAlbumName().toLowerCase().contains(finalName)) {
                return true;
            }
            if(album.getBaseAlbum().getArtist().toLowerCase().contains(finalName)) {
                return true;
            }
            return false;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    public static void postAlbumAction(Album album, MAlbumInitializeAction action) {
        AlbumAction albumAction = new AlbumAction();
        albumAction.album = album;
        albumAction.action = action;
        pendingAlbums.add(albumAction);
    }

    public static void processPendingAlbums() {
        if(pendingAlbums.isEmpty()) {
            return;
        }
        pendingAlbums.parallelStream().forEach(album -> {
            initializedAlbums.add(album.action.initializeAlbum(album.album));
        });
        pendingAlbums.clear();
    }
}
