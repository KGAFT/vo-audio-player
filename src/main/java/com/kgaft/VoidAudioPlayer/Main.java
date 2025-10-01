package com.kgaft.VoidAudioPlayer;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.kgaft.VoidAudioPlayer.Native.LibrarySerializer;

import com.kgaft.VoidAudioPlayer.Verbose.Album;
import com.kgaft.VoidAudioPlayer.Verbose.Artist;
import com.kgaft.VoidAudioPlayer.Verbose.LibraryParser;
import com.kgaft.VoidAudioPlayer.Verbose.Track;


import javax.swing.*;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


//Testing purposes only, in future will be organised!

public class Main {
    static {
        File file = new File("target/debug");
        for (File listFile : file.listFiles()) {
            if (listFile.getName().endsWith(".so") || listFile.getName().endsWith(".dylib") || listFile.getName().endsWith(".dll")) {
                try {
                    System.load(listFile.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void tryFindOrCreateArtist(Album album, List<Artist> artists) {
        Optional<Artist> artistOpt =  artists.stream().filter(artist -> artist.getName().equals(album.getArtist())).findFirst();
        if(artistOpt.isPresent()) {
            album.setArtistObject(artistOpt.get());
            artistOpt.get().getAlbums().add(album);
        } else {
            Artist artist = new Artist();
            artist.setName(album.getArtist());
            artist.setAlbums(new ArrayList<>());
            artist.getAlbums().add(album);
            album.setArtistObject(artist);
            artists.add(artist);
        }
    }

    public static ConnectionSource createConnection() throws Exception {
        // Use relative path -> file will be created next to your executable JAR
        String databaseUrl = "jdbc:sqlite:./mydatabase.db";
        return new JdbcConnectionSource(databaseUrl);
    }

    public static void main(String[] args) {
        List<Album> albumList = new ArrayList<>();

        /*
        LibraryParser.recurrentIterDirectory("C:/Users/kftg/Music/Test", albumList);
        albumList.sort(Comparator.comparing(Album::getName).reversed());
        List<Artist> artistList = new ArrayList<>();
        List<Track> trackList = new ArrayList<>();
        albumList.forEach(album -> {
            if(album.getArtist()!=null && !album.getArtist().isEmpty()){
                tryFindOrCreateArtist(album, artistList);
            }
            album.getTracks().forEach(track -> {
                track.setAlbumObject(album);
                trackList.add(track);
            });
        });

         */



        try (ConnectionSource connectionSource = createConnection()) {
            TableUtils.createTableIfNotExists(connectionSource, Artist.class);
            TableUtils.createTableIfNotExists(connectionSource, Album.class);
            TableUtils.createTableIfNotExists(connectionSource, Track.class);


            Dao<Track, Long> trackDao = DaoManager.createDao(connectionSource, Track.class);
            Dao<Album, Long> albumDao = DaoManager.createDao(connectionSource, Album.class);
            Dao<Artist, Long> artistDao = DaoManager.createDao(connectionSource, Artist.class);


            albumList.addAll(albumDao.queryForAll());

/*
            trackList.forEach(track -> {
                try {
                    trackDao.create(track);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            albumList.forEach(album -> {
                try {
                    albumDao.create(album);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            artistList.forEach(artist -> {
                try {
                    artistDao.create(artist);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });


             */


        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlayerWindow window = new PlayerWindow(albumList);

    }
}