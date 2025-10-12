package com.kgaft.VoidAudioPlayer.Native;

import java.util.List;

public class PlayerDsd{
    public static native List<String> enumerateSupportedDevices();
    public static native long initializePlayer(String deviceName);
    public static native void destroyPlayer(long handle);
    public static native void loadTrack(long handle, String path);
    public static native void playOnCurrentThread(long handle);
    public static native boolean seekTrack(long handle, float percent);
    public static native long getTrackLength(long handle);
    public static native float getTrackPos(long handle);
    public static native void setPlaying(long handle, boolean isPlaying);
    public static native boolean isPlaying(long handle);
    public static native void stop(long handle);

}
