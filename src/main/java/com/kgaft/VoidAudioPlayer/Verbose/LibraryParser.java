package com.kgaft.VoidAudioPlayer.Verbose;

import com.bunjlabs.jecue.CueLoader;
import com.bunjlabs.jecue.entities.CueSheet;
import com.bunjlabs.jecue.entities.CueTrackIndex;
import com.bunjlabs.jecue.entities.CueTrackInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

class CueParseResult {
    public List<String> artistNames;
    public List<File> filesToRemove;
    public List<Album> newAlbums;
}

public class LibraryParser {
    public static final String[] IMAGE_EXTENSIONS = new String[]{"jpg", "png", "jpeg", "gif", "tiff"};
    public static final String[] AUDIO_EXTENSIONS = {
            // Uncompressed / PCM
            ".wav", ".bwf", ".aiff", ".aif", ".aifc", ".au", ".snd", ".pcm", ".sd2",

            // Lossless compressed
            ".flac", ".alac", ".ape", ".wv", ".tta", ".tak", ".shn",
            ".ofr", ".ofs", ".la", ".caf", ".dsf", ".dff", ".m4a",

            // Lossy compressed
            ".mp3", ".aac", ".m4b", ".m4p", ".m4r", ".3gp", ".3g2",
            ".amr", ".ogg", ".oga", ".opus", ".spx", ".vqf", ".ra", ".wma", ".asf",

            // MIDI and related
            ".mid", ".midi", ".rmi", ".kar",
    };

    public static final HashMap<String, List<String>> filters;

    static {
        filters = new HashMap<>();
        filters.put("image", Arrays.stream(IMAGE_EXTENSIONS).toList());
        filters.put("audio", Arrays.stream(AUDIO_EXTENSIONS).toList());
        filters.put("cue", List.of(".cue"));
    }


    public static void recurrentIterDirectory(String path, List<Album> output) {

        File f = new File(path);
        if (!f.isDirectory()) {
            return;
        }
        inspectDir(new File(path), output);
        for (File file : Objects.requireNonNull(f.listFiles())) {
            if (file.isDirectory()) {
                recurrentIterDirectory(file.getAbsolutePath(), output);
            }
        }
        return;
    }

    public static void inspectDir(File directory, List<Album> output) {
        HashMap<String, List<File>> filesInfos = FileFilter.filterFiles(directory.listFiles(), filters);
        List<File> cueSheets = filesInfos.get("cue");
        List<String> artistNames = new ArrayList<>();
        List<Album> newAlbums = new ArrayList<>();
        if (cueSheets != null) {
            cueSheets.forEach(file1 -> {
                try {
                    CueLoader loader = new CueLoader(file1);
                    CueParseResult result = processCueSheet(loader.load(), filesInfos, output);
                    List<File> newAudioFiles = filesInfos.get("audio");
                    if (newAudioFiles != null) {
                        result.filesToRemove.forEach(newAudioFiles::remove);
                    }
                    filesInfos.put("audio", newAudioFiles);
                    artistNames.addAll(result.artistNames);
                    newAlbums.addAll(result.newAlbums);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        List<File> remainingTracks = filesInfos.get("audio");
        List<File> covers = filesInfos.get("image");
        if (remainingTracks != null) {
            List<Track> tracks = new ArrayList<>();
            remainingTracks.forEach(file1 -> {
                Track track = Track.getTrackInfo(file1.getAbsolutePath());
                tracks.add(track);
            });
            tryToFindOrCreateAlbum(tracks, output, newAlbums, true);
        }
        if (!output.isEmpty() && covers != null && !covers.isEmpty()) {
            int index = 0;

            while (!covers.isEmpty() || index < newAlbums.size()) {
                boolean found = false;
                while (index < newAlbums.size() && (newAlbums.get(index).getCover() != null && newAlbums.get(index).getCover().length > 0)) {
                    if (newAlbums.get(index).getCover() == null || newAlbums.get(index).getCover().length == 0) {
                        found = true;
                    }
                    index++;
                }
                if (!found && index >= newAlbums.size()) {
                    break;
                }
                try {
                    if (covers.isEmpty()) {
                        break;
                    }
                    newAlbums.get(index).setCover(Files.readAllBytes(covers.getFirst().toPath()));
                    covers.remove(0);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static CueParseResult processCueSheet(CueSheet sheet, HashMap<String, List<File>> files, List<Album> albums) {
        List<String> artistNames = new ArrayList<>();
        List<File> filesToRemove = new ArrayList<>();
        List<Album> newAlbums = new ArrayList<>();
        sheet.getFiles().forEach(cueFile -> {
            List<Track> tracks = new ArrayList<>();
            File musicFile = tryFindCueRelatedFile(files, cueFile.getFileName());
            if (musicFile != null) {
                Track baseTrack = Track.getTrackInfo(musicFile.getAbsolutePath());
                for (int i = 0; i < cueFile.getTracks().size(); i++) {
                    CueTrackInfo info = cueFile.getTracks().get(i);
                    CueTrackIndex index2 = getIndex(info.getIndexes());
                    long offset = (index2.getMinutes() * 60L + index2.getSeconds()) * 1000;
                    long duration = 0;
                    if (i != cueFile.getTracks().size() - 1) {
                        CueTrackInfo info2 = cueFile.getTracks().get(i + 1);
                        CueTrackIndex endIndex = getIndex(info2.getIndexes());
                        duration = (endIndex.getMinutes() * 60L + endIndex.getSeconds()) * 1000 - offset;
                    } else {
                        duration = baseTrack.getDurationMs() - offset;
                    }
                    Track track = new Track(baseTrack);
                    if(baseTrack.getAlbumName() == null || baseTrack.getAlbumName().isEmpty()){
                        track.setAlbumName(musicFile.getName());
                        if(track.getAlbumName()==null || track.getAlbumName().isEmpty()){
                            track.setAlbumName(info.getSongwriter());
                            if(track.getAlbumName()==null ||track.getAlbumName().isEmpty()){
                                track.setAlbumName(musicFile.getName());
                            }
                        }
                    }
                    if(baseTrack.getArtistName() ==null || baseTrack.getArtistName().isEmpty()){
                        track.setArtistName(info.getPerformer());
                        if(track.getArtistName()==null || track.getArtistName().isEmpty()){
                            track.setArtistName(info.getSongwriter());
                            if(track.getArtistName()==null ||track.getArtistName().isEmpty()){
                                track.setArtistName(track.getAlbumName());
                            }
                        }
                    }
                    track.setName(info.getTitle());
                    track.setOffsetMs(offset);
                    track.setDurationMs(duration);
                    tracks.add(track);
                }
                if (!tracks.isEmpty()) {
                    tryToFindOrCreateAlbum(tracks, albums, newAlbums, false);
                    artistNames.add(tracks.getFirst().getArtistName());
                    filesToRemove.add(musicFile);
                } else {
                    tracks.add(baseTrack);
                    tryToFindOrCreateAlbum(tracks, albums, newAlbums, false);
                    artistNames.add(tracks.getFirst().getArtistName());
                    filesToRemove.add(musicFile);
                    //         System.out.println(musicFile.getAbsolutePath());
                }

            }
        });
        CueParseResult result = new CueParseResult();
        result.newAlbums = newAlbums;
        result.artistNames = artistNames;
        result.filesToRemove = filesToRemove;
        return result;
    }

    public static void tryToFindOrCreateAlbum(List<Track> tracks, List<Album> albums, List<Album> newAlbums, boolean sortTracks) {
        tracks.forEach(track -> {
            String album = track.getAlbumName();
            if (album.isEmpty()) {
                album = "uncategorized";
                track.setAlbumName(album);
            }
            boolean found = false;
            for (Album album1 : albums) {
                if (album1.getName().equals(album)) {
                    found = true;
                    album1.getTracks().add(track);
                    if(sortTracks) {
                        album1.sortTracks();
                    }
                    break;
                }
            }
            if (!found) {
                Album album1 = new Album();
                album1.setTracksList(true);
                album1.setName(album);
                album1.setArtist(track.getArtistName());
                album1.setCover(track.getPictureBytes());
                album1.setGenre(track.getGenre());
                album1.setYear(track.getYear());
                album1.getTracks().add(track);
                if(sortTracks) {
                    album1.sortTracks();
                }
                albums.add(album1);
                newAlbums.add(album1);
            }
        });
    }

    public static File tryFindCueRelatedFile(HashMap<String, List<File>> files, String name) {
        List<File> audioFiles = files.get("audio");
        if (audioFiles == null || audioFiles.isEmpty()) return null;
        for (File element : audioFiles) {
            if (element.getName().equals(name)) {
                return element;
            }
        }
        return null;
    }

    public static CueTrackIndex getIndex(Map<Integer, CueTrackIndex> indexes) {
        int index = 1;
        CueTrackIndex index2 = indexes.get(index);
        if(index2 == null) {
            index = 0;
        }
        while (index2 == null) {
            index2 = indexes.get(index);
            index++;
        }
        return index2;
    }
}
