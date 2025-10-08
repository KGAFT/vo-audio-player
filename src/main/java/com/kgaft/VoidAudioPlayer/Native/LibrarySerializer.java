package com.kgaft.VoidAudioPlayer.Native;

import com.kgaft.VoidAudioPlayer.Verbose.Album;

import java.util.List;

public class LibrarySerializer {
    public static native byte[] serialize(List<Album> albumList);
    public static native List<Album> deserialize(byte[] data);
    public static native void serializeAndSaveToFile(List<Album> albumList, String destinationFullPath);
    public static native List<Album> deserializeFromFile(String sourceFullPath);
}
