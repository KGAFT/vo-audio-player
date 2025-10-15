package com.kgaft.VoidAudioPlayer.Verbose;
public class CueSong {
    public long offset;
    public String title;
    public long duration;
    public String performer;

    public CueSong(long offset, String title, long duration, String performer) {
        this.offset = offset;
        this.title = title;
        this.duration = duration;
        this.performer = performer;
    }

    public CueSong() {
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
    }
}