package com.kgaft.VoidAudioPlayer.Model;

import com.kgaft.VoidAudioPlayer.Verbose.Album;
import com.kgaft.VoidAudioPlayer.Verbose.Artist;
import com.kgaft.VoidAudioPlayer.Verbose.LibraryIndex;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import java.util.List;

public class MCollection {
    private static LibraryIndex index;
    public static void init(LibraryIndex index){
        MCollection.index = index;
    }


    public static List<Album> getAlbums(){
        return index.getAlbums();
    }
    public static List<Artist> getArtists(){
        return index.getArtists();
    }
    public static  List<Track> getTracks() {
        return index.getTracks();
    }
}
