package com.kgaft.VoidAudioPlayer.Native;

import java.util.List;

public class Player {
    public static native List<String> getDevices();
    public static native long initializePlayer();
    public static native void setDevice(long handle, String device);
    public static native void loadTrack(long handle, String path);
    public static native void pollEventsMain(long handle);
    public static native boolean seekTrack(long handle, float seconds);
    public static native long getTrackLength(long handle);
    public static native long getTrackPos(long handle);
    public static native void setPlaying(long handle, boolean isPlaying);
    public static native boolean isPlaying(long handle);
    public static native void stop(long handle);
}
