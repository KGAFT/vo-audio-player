package com.kgaft.VoidAudioPlayer.Verbose;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

//@DatabaseTable(tableName = "indexed_directories")
public class IndexedDirectory {
    @DatabaseField(generatedId = true)
    private long id;
    @DatabaseField(unique = true)
    private String path;
    @DatabaseField
    private int childrenAmount;
    @DatabaseField
    private String[] fileNames;
    @DatabaseField
    private Long[] fileSizes;
    @DatabaseField
    private String[] fileHashes;

    public IndexedDirectory(long id, String path, int childrenAmount, Long[] fileSizes, String[] fileHashes) {
        this.id = id;
        this.path = path;
        this.childrenAmount = childrenAmount;
        this.fileSizes = fileSizes;
        this.fileHashes = fileHashes;
    }

    public IndexedDirectory() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String[] getFileNames() {
        return fileNames;
    }

    public void setFileNames(String[] fileNames) {
        this.fileNames = fileNames;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getChildrenAmount() {
        return childrenAmount;
    }

    public void setChildrenAmount(int childrenAmount) {
        this.childrenAmount = childrenAmount;
    }

    public Long[] getFileSizes() {
        return fileSizes;
    }

    public void setFileSizes(Long[] fileSizes) {
        this.fileSizes = fileSizes;
    }

    public String[] getFileHashes() {
        return fileHashes;
    }

    public void setFileHashes(String[] fileHashes) {
        this.fileHashes = fileHashes;
    }
}
