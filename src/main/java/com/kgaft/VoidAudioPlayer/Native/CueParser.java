package com.kgaft.VoidAudioPlayer.Native;

import com.kgaft.VoidAudioPlayer.Verbose.CueAlbum;

import java.util.List;

public class CueParser {
    public static native List<CueAlbum> parseCueFile(String filePath);
}
