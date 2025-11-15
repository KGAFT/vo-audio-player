package com.kgaft.VoidAudioPlayer.Verbose;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LibraryIndex {
    private static ConnectionSource connection = null;
    private static String databaseUrl;

    public static String getDatabaseUrl() {
        return databaseUrl;
    }

    public static void setDatabaseUrl(String databaseUrl) {
        LibraryIndex.databaseUrl = "jdbc:sqlite:"+databaseUrl;
    }

    private static ConnectionSource getConnection() {
        if (connection == null) {
            try {
                connection = new JdbcConnectionSource(databaseUrl);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return connection;
    }

    private Dao<Track, Long> trackDao;
    private Dao<Album, Long> albumDao;
    private Dao<Artist, Long> artistDao;
    private Dao<IndexedDirectory, Long> indexedDirectoryDao;
    public static LibraryIndex getInstance() {
        return new LibraryIndex(getConnection());
    }

    public LibraryIndex(ConnectionSource connection) {
        try {
            TableUtils.createTableIfNotExists(connection, Artist.class);
            TableUtils.createTableIfNotExists(connection, Album.class);
            TableUtils.createTableIfNotExists(connection, Track.class);

            trackDao = DaoManager.createDao(connection, Track.class);
            albumDao = DaoManager.createDao(connection, Album.class);
            artistDao = DaoManager.createDao(connection, Artist.class);
            indexedDirectoryDao = DaoManager.createDao(connection, IndexedDirectory.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void tryFindOrCreateArtist(Album album, List<Artist> artists) {
        try {
            List<Artist> artistsList = artistDao.queryForEq("name", album.getArtist());
            if(!artistsList.isEmpty()){
                album.setArtist(artistsList.getFirst().getName());
                album.setArtistObject(artistsList.getFirst());
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    private boolean checkDirectory(String path){
        try {
            Optional<IndexedDirectory> res = indexedDirectoryDao.queryForEq("path", path).stream().findAny();
            if(res.isPresent()){
                IndexedDirectory directory = res.get();
                if(directory.recheckNeeded()){
                    directory = new IndexedDirectory(directory.getPath());
                    indexedDirectoryDao.createOrUpdate(directory);
                    return true;
                }
                return false;
            } else {
                IndexedDirectory directory = new IndexedDirectory(path);
                indexedDirectoryDao.createOrUpdate(directory);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void findUncheckedDirectory(String path){

    }


    public void addDirectory(String directory) {
        List<Album> albumList = new ArrayList<>();
        LibraryParser.recurrentIterDirectory(directory, albumList);
        albumList.sort(Comparator.comparing(Album::getName).reversed());
        List<Artist> artistList = new ArrayList<>();
        List<Track> trackList = new ArrayList<>();
        albumList.forEach(album -> {
            album.processDuration();
            if(album.getArtist()!=null && !album.getArtist().isEmpty()){
                tryFindOrCreateArtist(album, artistList);
            }
            boolean albumfound = false;
            try {
                List<Album> albums = albumDao.queryForEq("name", album.getName());
                if(albums.size()>0) {
                    Album newAlb = albums.getFirst();
                    newAlb.setTracks((List<Track>) album.getTracks());
                    album.getTracks().forEach(track -> {
                        if(!newAlb.getTracks().contains(track)) {
                            track.setAlbumObject(newAlb);
                            trackList.add(track);
                        }
                    });
                    album.setId(newAlb.getId());
                    albumfound = true;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            if(!albumfound){
                album.getTracks().forEach(track -> {
                    track.setAlbumObject(album);
                    trackList.add(track);
                });
            }
        });

        trackList.forEach(track -> {
            if(track.getPath().endsWith(".dff")){
                System.err.println(track.getAlbumName()+track.getPath());
            }
            try {
                trackDao.create(track);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<Album> getAlbums() {
        try {
            return albumDao.queryBuilder().orderBy("name", true).query();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Artist> getArtists() {
        try {
            return artistDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Track> getTracks() {
        try {
            return trackDao.queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}
