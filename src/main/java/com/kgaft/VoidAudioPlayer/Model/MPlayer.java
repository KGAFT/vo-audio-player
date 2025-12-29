package com.kgaft.VoidAudioPlayer.Model;

import com.kgaft.VoidAudioPlayer.Native.Player;
import com.kgaft.VoidAudioPlayer.Native.PlayerDsd;
import com.kgaft.VoidAudioPlayer.Verbose.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


public class MPlayer {
    private static long nativePlayer = 0;
    private static long dsdPlayer = 0;
    private static MPlayList playList = new MPlayList();
    private static DsdDeviceInfo selectedDsdDevice = null;
    private static volatile boolean isPlaying = false;
    private static Track currentTrack = null;
    private static volatile boolean isDsd = false;
    private static Thread playThread = null;
    private static Thread dsdPlayBack = null;
    public static List<String> enumerateDevices() {
        return Player.getDevices();
    }

    private static volatile int currentTrackIndex = -1;

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
        new Thread(() -> {
            Player.pollEventsMain(nativePlayer);
        }).start();
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

    public static void startPlaySession() {
        if(playThread==null){
            playThread = new Thread(() -> {
                while (true) {
                    if (isPlaying) {
                        if (isDsd) {
                            if (!PlayerDsd.isPlaying(dsdPlayer)) {
                                if (pushPlayListForward())
                                    processTrack(playList.getTrackList().get(currentTrackIndex));
                                else
                                    isPlaying = false;
                            }
                        } else {
                            if (!Player.isPlaying(nativePlayer)) {
                                if (pushPlayListForward())
                                    processTrack(playList.getTrackList().get(currentTrackIndex));
                                else
                                    isPlaying = false;
                            }
                        }
                    }
                }
            });
            playThread.start();
        }

    }

    private static boolean pushPlayListForward() {
        currentTrackIndex += 1;
        return currentTrackIndex < playList.getTrackList().size();
    }

    private static void processTrack(Track track) {
        if (processDsdOperations(track)) {
            PlayerDsd.loadTrack(dsdPlayer, track.getPath());
            PlayerDsd.setPlaying(dsdPlayer, true);

            dsdPlayBack = new Thread(()->{
                PlayerDsd.playOnCurrentThread(dsdPlayer);
            });
            dsdPlayBack.start();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (track.getOffsetMs() > 0) {
                PlayerDsd.seekTrack(dsdPlayer, track.getOffsetMs());
            }
        } else {
            Player.loadTrack(nativePlayer, track.getPath());
            Player.setPlaying(nativePlayer, true);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (track.getOffsetMs() > 0) {
                float seek = (float) track.getOffsetMs() / track.getAlbumDurationMs();
                Player.seekTrack(nativePlayer, seek);
            }
        }
    }

    public static void setPlayListPos(int pos) {

        startPlaySession();
        currentTrackIndex = pos-1;
        if(isDsd){
            PlayerDsd.stop(dsdPlayer);
        } else {
            Player.stop(nativePlayer);
        }
        isPlaying = true;
    }

    public static float getPosition() {
        float pos = 0;
        if (isDsd) {
            pos = PlayerDsd.getTrackPos(dsdPlayer);
            if (currentTrack.getAlbumDurationMs() == 0 || currentTrack.getOffsetMs() == 0) {
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
        if (currentTrack.getOffsetMs() == 0 || currentTrack.getAlbumDurationMs() == 0) {
            seek = pos;
        } else {
            long tempPos = (long) (currentTrack.getOffsetMs() + currentTrack.getDurationMs() * pos);
            seek = (float) tempPos / (float) currentTrack.getAlbumDurationMs();
        }
        if (isDsd) {
            PlayerDsd.seekTrack(dsdPlayer, seek);
        } else {
            Player.seekTrack(nativePlayer, seek);
        }
    }

    public static void nextTrack() {
        if(isDsd){
            PlayerDsd.stop(dsdPlayer);
        } else {
            Player.stop(nativePlayer);
        }
    }

    public static void previousTrack() {
        currentTrackIndex-=2;
        if(isDsd){
            PlayerDsd.stop(dsdPlayer);
        } else {
            Player.stop(nativePlayer);
        }
    }

    public static void pause() {
        isPlaying = false;
        if (isDsd) {
            PlayerDsd.setPlaying(dsdPlayer, false);
        } else {
            Player.setPlaying(nativePlayer, false);
        }
    }

    public static void play() {
        startPlaySession();
        if (isDsd) {
            PlayerDsd.setPlaying(dsdPlayer, true);
        } else {
            Player.setPlaying(nativePlayer, true);
        }
        isPlaying = true;
    }

    public static boolean isPlaying() {
        return MPlayer.isPlaying;
    }

    private static boolean processDsdOperations(Track track) {
        if ((track.getPath().toLowerCase().endsWith(".dsf") || track.getPath().toLowerCase().endsWith(".dff")) && selectedDsdDevice != null) {
            if (dsdPlayer == 0) {
                dsdPlayer = PlayerDsd.initializePlayer(selectedDsdDevice.getName());
                isDsd = true;
            }
            return true;
        } else if (selectedDsdDevice != null) {
            if (dsdPlayer != 0) {
                PlayerDsd.destroyPlayer(dsdPlayer);
                dsdPlayer = 0;
            }
            isDsd = false;
        }
        return false;
    }
}
