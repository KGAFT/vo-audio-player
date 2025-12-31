package com.kgaft.VoidAudioPlayer.Verbose;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.SelectArg;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.kgaft.VoidAudioPlayer.Model.MSettings;
import com.kgaft.VoidAudioPlayer.Ui.ProgressAcceptor;

import java.sql.SQLException;
import java.util.*;

public class LibraryIndex {
    private static ConnectionSource connection = null;
    private static String databaseUrl;
    private static List<LibraryIndex> instances = new ArrayList<>();

    public static String getDatabaseUrl() {
        return databaseUrl;
    }

    public static void setDatabaseUrl(String databaseUrl) {
        LibraryIndex.databaseUrl = "jdbc:sqlite:"+databaseUrl;
        try {
            updateConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        instances.forEach(instance -> {
            instance.initRoutine(getConnection());
        });
    }
    private static void updateConnection() throws SQLException {
        connection = new JdbcConnectionSource(databaseUrl);
    }

    private static ConnectionSource getConnection() {
        if (connection == null) {
            try {
                updateConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return connection;
    }

    private Dao<Track, Long> trackDao;
    private Dao<Album, Long> albumDao;
    private Dao<Artist, Long> artistDao;
    private Dao<Image, Long> imagesDao;
    private Dao<IndexedDirectory, Long> indexedDirectoryDao;
    public static LibraryIndex getInstance() {
        LibraryIndex index = new LibraryIndex(getConnection());
        instances.add(index);
        return index;
    }

    public LibraryIndex(ConnectionSource connection) {
        initRoutine(connection);
    }
    private void initRoutine(ConnectionSource connection){
        try {
            TableUtils.createTableIfNotExists(connection, Artist.class);
            TableUtils.createTableIfNotExists(connection, Image.class);
            TableUtils.createTableIfNotExists(connection, Album.class);
            TableUtils.createTableIfNotExists(connection, Track.class);


            trackDao = DaoManager.createDao(connection, Track.class);
            albumDao = DaoManager.createDao(connection, Album.class);
            artistDao = DaoManager.createDao(connection, Artist.class);
            indexedDirectoryDao = DaoManager.createDao(connection, IndexedDirectory.class);
            imagesDao = DaoManager.createDao(connection, Image.class);
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

    private void processAlbumImages(List<Album> albumList, HashSet<Image> existingImages){
        albumList.forEach(album1 -> {
            if(album1.getCover()==null){
                album1.setCover(new Image(0, new byte[]{}));
            }
            existingImages.add(album1.getCover());
            Image existing = existingImages.stream()
                    .filter(img -> img.equals(album1.getCover()))
                    .findFirst().get();
            try {
                imagesDao.createIfNotExists(existing);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            album1.setCover(existing);
        });
    }

    private void processTracksImages(List<Track> tracks, HashSet<Image> existingImages){
        tracks.forEach(track -> {
            if(track.getImage()==null){
                if(track.getPictureBytes()!=null && track.getPictureBytes().length!=0){
                    Image obj = new Image(0, track.getPictureBytes());
                    track.setImage(obj);
                    track.setPictureBytes(null);
                } else {
                    track.setImage(new Image(0, new byte[]{}));
                }
            }
            existingImages.add(track.getImage());
            Image existing = existingImages.stream()
                    .filter(img -> img.equals(track.getImage()))
                    .findFirst().get();
            try {
                imagesDao.createIfNotExists(existing);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            track.setImage(existing);
        });
    }

    public void addDirectory(String directory, ProgressAcceptor progressInfo) {
        List<Album> albumList = new ArrayList<>();
        LibraryParser.recurrentIterDirectory(directory, albumList, progressInfo);
        albumList.sort(Comparator.comparing(Album::getName).reversed());
        List<Artist> artistList = new ArrayList<>();
        List<Track> trackList = new ArrayList<>();
        HashSet<Image> uniqueImages = new HashSet<>();
        processTracksImages(trackList, uniqueImages);
        processAlbumImages(albumList, uniqueImages);


        albumList.forEach(album -> {
            album.processDuration();
            if(album.getArtist()!=null && !album.getArtist().isEmpty()){
                tryFindOrCreateArtist(album, artistList);
            }
            boolean albumfound = false;
            try {
                QueryBuilder<Album, Long> qb = albumDao.queryBuilder();
                qb.where().eq("name", new SelectArg(album.getName()));
                List<Album> albums = albumDao.query(qb.prepare());
                if(albums.size()>0) {
                    albumfound = true;
                    Album newAlb = albums.getFirst();
                    newAlb.setTracks((List<Track>) album.getTracks());
                    album.getTracks().forEach(track -> {
                        if(!newAlb.getTracks().contains(track)) {
                            track.setAlbumObject(newAlb);
                            trackList.add(track);
                        }
                    });
                    album.setId(newAlb.getId());

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
            return artistDao.queryBuilder().orderBy("name", true).query();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Track> getTracks() {
        try {
            return trackDao.queryBuilder().orderBy("name", true).query();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

}
