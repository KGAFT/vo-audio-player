package com.kgaft.VoidAudioPlayer.Verbose;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@DatabaseTable(tableName = "albums")
public class Album {
    @DatabaseField(generatedId = true)
    private long id;
    @DatabaseField
    private String artist = "";
    @DatabaseField(dataType = com.j256.ormlite.field.DataType.BYTE_ARRAY)
    private byte[] cover;
    @DatabaseField(canBeNull = false, unique = true)
    private String name = "";
    @ForeignCollectionField(eager = false)
    private Collection<Track> tracks;
    @DatabaseField
    private int year;
    @DatabaseField
    private String genre = "";
    @DatabaseField
    private long duration = 0;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private Artist artistObject;

    private boolean tracksList = false;

    public Album(byte[] cover, String name, List<Track> tracks) {
        this.cover = cover;
        this.name = name;
        this.tracks = tracks;
        this.tracksList = true;
    }

    public Album() {
        this.tracks = new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public void setTracksList(boolean tracksList) {
        this.tracksList = tracksList;
    }

    public Artist getArtistObject() {
        return artistObject;
    }

    public void setArtistObject(Artist artistObject) {
        this.artistObject = artistObject;
    }

    public byte[] getCover() {
        return cover;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        if (duration == 0) {
            long curDur = 0;
            for (Track track : tracks) {
                curDur += track.getDurationMs();
            }
            duration = curDur;
            return curDur;
        }
        return duration;
    }

    public void processDuration() {
        if(duration == 0){
            long curDur = 0;
            for (Track track : tracks) {
                curDur += track.getDurationMs();
            }
            duration = curDur;
        }
        if(duration == 0){
            System.err.println("DURATION_ERROR: "+name);
        }
        tracks.forEach(track -> track.setAlbumDurationMs(duration));
    }

    public void sortTracks() {
        if (tracksList) {
            ((List<Track>) tracks).sort(Comparator.comparing(track -> new File(track.getPath()).getName()));
        }
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

    public Collection<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
