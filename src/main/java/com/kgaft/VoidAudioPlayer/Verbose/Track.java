package com.kgaft.VoidAudioPlayer.Verbose;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "tracks")
public class Track {
    //Heavily memory load, use only for displaying about one track
    public static native Track getTrackInfo(String path);
    @DatabaseField(generatedId = true)
    private long id;
    @DatabaseField(canBeNull = false)
    private String name;
    @DatabaseField
    private String albumName;
    @DatabaseField
    private String artistName;
    @DatabaseField
    private String genre;
    @DatabaseField
    private String path;
    @DatabaseField
    private int year;
    @DatabaseField
    private byte bitDepth;
    @DatabaseField
    private int sampleRate;
    @DatabaseField
    private byte channels;
    @DatabaseField
    private int overallBitrate;
    @DatabaseField(dataType = com.j256.ormlite.field.DataType.BYTE_ARRAY)
    private byte[] pictureBytes;
    @DatabaseField
    private long offsetMs;
    @DatabaseField
    private long durationMs;
    @DatabaseField(foreign = true, foreignAutoRefresh = true, foreignAutoCreate = true)
    private Album albumObject;

    public Track(String name, String albumName, String artistName, String genre, String path) {
        this.name = name;
        this.albumName = albumName;
        this.artistName = artistName;
        this.genre = genre;
        this.path = path;
    }

    public Track(Track track){
        this.name = track.name;
        this.albumName = track.albumName;
        this.artistName = track.artistName;
        this.genre = track.genre;
        this.path = track.path;
        this.year = track.year;
        this.bitDepth = track.bitDepth;
        this.sampleRate = track.sampleRate;
        this.channels = track.channels;
        this.overallBitrate = track.overallBitrate;
        this.pictureBytes = track.pictureBytes;
        this.offsetMs = track.offsetMs;
        this.durationMs = track.durationMs;

    }

    public Track() {
    }

    public byte[] getPictureBytes() {
        return pictureBytes;
    }

    public void setPictureBytes(byte[] pictureBytes) {
        this.pictureBytes = pictureBytes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlbumName() {
        return albumName;
    }

    public long getOffsetMs() {
        return offsetMs;
    }

    public void setOffsetMs(long offsetMs) {
        this.offsetMs = offsetMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public Album getAlbumObject() {
        return albumObject;
    }

    public void setAlbumObject(Album albumObject) {
        this.albumObject = albumObject;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public byte getBitDepth() {
        return bitDepth;
    }

    public void setBitDepth(byte bitDepth) {
        this.bitDepth = bitDepth;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public byte getChannels() {
        return channels;
    }

    public void setChannels(byte channels) {
        this.channels = channels;
    }

    public int getOverallBitrate() {
        return overallBitrate;
    }

    public void setOverallBitrate(int overallBitrate) {
        this.overallBitrate = overallBitrate;
    }
}
