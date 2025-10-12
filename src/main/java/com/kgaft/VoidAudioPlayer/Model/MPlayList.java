package com.kgaft.VoidAudioPlayer.Model;

import com.kgaft.VoidAudioPlayer.Verbose.Track;

import java.util.ArrayList;
import java.util.List;

public class MPlayList {
    private List<Track> trackList = new ArrayList<>();
    public void pushTrack(Track track){
        trackList.add(track);
    }
    public void removeTrack(int index){
        if(trackList.size()>index){
            trackList.remove(index);
        }
    }
    public void addTrackBetween(int startIndex, Track track){
        trackList.add(startIndex, track);
    }
    protected List<Track> getTrackList(){
        return trackList;
    }
}
