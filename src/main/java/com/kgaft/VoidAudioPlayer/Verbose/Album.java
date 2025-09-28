package com.kgaft.VoidAudioPlayer.Verbose;

import com.kgaft.VoidAudioPlayer.Native.Track;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Album {
    private String artist;
    private byte[] cover;
    private String name;
    private List<Track> tracks = new ArrayList<>();
    private int year;
    private String genre;

    public Album(byte[] cover, String name, List<Track> tracks) {
        this.cover = cover;
        this.name = name;
        this.tracks = tracks;
    }

    public Album() {
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public byte[] getCover() {
        return cover;
    }

    public void sortTracks() {
        tracks.sort(Comparator.comparing(track -> new File(track.getPath()).getName()));
    }

    public void setCover(byte[] cover) {
        this.cover = cover;
        if (tracks != null && tracks.size() > 0) {
            tracks.forEach(track -> {
                if (track.getPictureBytes() == null) {
                    track.setPictureBytes(cover);
                }
            });
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }
}
