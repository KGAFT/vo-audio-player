package com.kgaft.VoidAudioPlayer.Model;

import com.kgaft.VoidAudioPlayer.Native.Player;
import com.kgaft.VoidAudioPlayer.Native.PlayerDsd;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import java.util.ArrayList;
import java.util.List;


public class MPlayer {
    private static long nativePlayer = 0;
    private static long dsdPlayer = 0;
    private static MPlayList playList = new MPlayList();
    private static String loadedTrackPath = "";
    private static DsdDeviceInfo selectedDsdDevice = null;
    private static boolean dsdPlaying = false;
    private static Track currentTrack = null;

    public static List<String> enumerateDevices() {
        return Player.getDevices();
    }

    public static List<DsdDeviceInfo> enumerateDsdDevices() {
        List<DsdDeviceInfo> dsdDeviceInfoList = new ArrayList<>();
        PlayerDsd.enumerateSupportedDevices().forEach(device -> {
            String[] args = device.split("/:/");
            DsdDeviceInfo dsdDeviceInfo = new DsdDeviceInfo();
            dsdDeviceInfo.setDescription(args[1]);
            dsdDeviceInfo.setName(args[0]);
            dsdDeviceInfoList.add(dsdDeviceInfo);
        });
        return dsdDeviceInfoList;
    }

    public static void initNativePlayer() {
        nativePlayer = Player.initializePlayer();
    }

    public static void pickDevice(String deviceName) {
        if (nativePlayer == 0) {
            initNativePlayer();
        }
        Player.setDevice(nativePlayer, deviceName);
    }

    public static void initDsdDevice(DsdDeviceInfo dsdDeviceInfo) {
        selectedDsdDevice = dsdDeviceInfo;
    }

    public static MPlayList getPlayList() {
        return playList;
    }

    public static void startPlaying(int playListStartIndex) {
        if (nativePlayer == 0) {
            initNativePlayer();
        }
        Track track = playList.getTrackList().get(playListStartIndex);
        currentTrack = track;
        String path = track.getPath().replace('\\', '/');
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (processDsdOperations(track)) {
            if (!loadedTrackPath.equals(path)) {
                PlayerDsd.loadTrack(dsdPlayer, track.getPath());
                loadedTrackPath = path;
            }
            dsdPlaying = true;
            new Thread(() -> {
                PlayerDsd.playOnCurrentThread(dsdPlayer);
            }).start();
            PlayerDsd.seekTrack(dsdPlayer, (float) track.getOffsetMs() / (float) track.getAlbumDurationMs());
        } else {
            if (!loadedTrackPath.equals(path)) {
                Player.loadTrack(nativePlayer, path);
                loadedTrackPath = path;
            }
            dsdPlaying = false;
            Player.setPlaying(nativePlayer, true);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Player.seekTrack(nativePlayer, track.getOffsetMs() / (float) track.getAlbumDurationMs());


        }
    }

    public static float getPosition() {
        float pos = 0;
        if (dsdPlaying) {
            pos = PlayerDsd.getTrackPos(dsdPlayer);
            if (currentTrack.getOffsetMs() == 0) {
                return pos;
            }
            return (pos * currentTrack.getAlbumDurationMs() - (float) currentTrack.getOffsetMs()) / (float) currentTrack.getDurationMs();
        } else {
            float localPos = (float) (Player.getTrackPos(nativePlayer) - currentTrack.getOffsetMs());

            return localPos / (float) currentTrack.getDurationMs();
        }
    }

    public static void seekTrack(float pos) {
        float seek = 0;
        if(currentTrack.getOffsetMs() == 0) {
            seek = pos;
        } else {
            long tempPos = (long) (currentTrack.getOffsetMs() + currentTrack.getDurationMs()*pos);
            seek = (float)tempPos/(float)currentTrack.getAlbumDurationMs();
        }
        if(dsdPlaying){
            PlayerDsd.seekTrack(nativePlayer, seek);
        } else {
            Player.seekTrack(nativePlayer, seek);
        }
    }

    public static void pause(){
        if(dsdPlaying){
            PlayerDsd.setPlaying(nativePlayer, false);
        } else {
            Player.setPlaying(nativePlayer, false);
        }
    }

    public static void play(){
        if(dsdPlaying){
            PlayerDsd.setPlaying(nativePlayer, true);
        } else {
            Player.setPlaying(nativePlayer, true);
        }
    }

    private static boolean processDsdOperations(Track track) {
        if ((track.getPath().toLowerCase().endsWith(".dsf") || track.getPath().toLowerCase().endsWith(".dff")) && selectedDsdDevice != null) {
            if (dsdPlayer == 0) {
                dsdPlayer = PlayerDsd.initializePlayer(selectedDsdDevice.getName());
            }
            return true;
        } else if (selectedDsdDevice != null) {
            if (dsdPlayer != 0) {
                PlayerDsd.destroyPlayer(dsdPlayer);
                dsdPlayer = 0;
            }
        }
        return false;
    }
}
