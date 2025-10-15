package com.kgaft.VoidAudioPlayer.Verbose;

import java.util.List;

public class CueAlbum {
    public String title;
    public String path;
    public long duration;
    public List<CueSong> songs;
    public String performer;

    public CueAlbum(String title, String path, long duration, List<CueSong> songs, String performer) {
        this.title = title;
        this.path = path;
        this.duration = duration;
        this.songs = songs;
        this.performer = performer;
    }

    public CueAlbum() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public List<CueSong> getSongs() {
        return songs;
    }

    public void setSongs(List<CueSong> songs) {
        this.songs = songs;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
    }
}