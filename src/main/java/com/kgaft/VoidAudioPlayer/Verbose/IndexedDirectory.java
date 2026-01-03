package com.kgaft.VoidAudioPlayer.Verbose;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.kgaft.VoidAudioPlayer.Util.HashMapSer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@DatabaseTable(tableName = "indexed_directories")
public class IndexedDirectory {
    @DatabaseField(id = true)
    private long id;
    @DatabaseField(unique = true)
    private String path;
    @DatabaseField
    private int childrenAmount;

   // private byte[] fileModifiedDates;
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    private HashMap<String, Long> fileModifiedDatesMap = null;
    public IndexedDirectory(long id, String path, int childrenAmount, HashMap<String, Long> fileModifiedDates) {
        this.id = id;
        this.path = path;
        this.childrenAmount = childrenAmount;
        setFileModifiedDates(fileModifiedDates);
    }

    public IndexedDirectory(String path){
        this.id = path.hashCode();
        this.path = path;
        File file = new File(path);
        File[] children = Objects.requireNonNull(file.listFiles());
        this.childrenAmount = children.length;
        HashMap<String, Long> fileModifiedDatesMap = new HashMap<>();
        for (File child : Objects.requireNonNull(children)) {
            fileModifiedDatesMap.put(child.getAbsolutePath(), child.lastModified());
        }
        setFileModifiedDates(fileModifiedDatesMap);
    }

    public IndexedDirectory() {
    }

    public boolean recheckNeeded(){
        File file = new File(path);
        File[] children = Objects.requireNonNull(file.listFiles());
        HashMap<String, Long> curModDates = new HashMap<>();
        for (File child : children) {
            curModDates.put(child.getAbsolutePath(), child.lastModified());
        }

        HashMap<String, Long> fileModifiedDates = getFileModifiedDates();
        if(curModDates.size() != fileModifiedDates.size()){
            return true;
        }
        for (Map.Entry<String, Long> entry : curModDates.entrySet()) {
            String key = entry.getKey();
            Long cDate = entry.getValue();
            Long date = fileModifiedDates.get(key);
            if (date == null) {
                return true;
            }
            if (!date.equals(cDate)) {
               return true;
            }
        }
        return false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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


    public HashMap<String, Long> getFileModifiedDates() {
        if(fileModifiedDatesMap == null){
            /*
            try {
           //     fileModifiedDatesMap = HashMapSer.deserialize(fileModifiedDates);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

             */
        }
        return fileModifiedDatesMap;
    }

    public void setFileModifiedDates(HashMap<String, Long> fileModifiedDates) {
       this.fileModifiedDatesMap = fileModifiedDates;
        /*
        try {
       //     this.fileModifiedDates = HashMapSer.serialize(this.fileModifiedDatesMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

         */
    }

    @Override
    public int hashCode() {
        return (int) this.id;
    }
}
