package com.kgaft.VoidAudioPlayer.Verbose;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Arrays;

@DatabaseTable(tableName = "images")
public class Image {


    @DatabaseField(id = true)
    private long id;
    @DatabaseField(dataType = com.j256.ormlite.field.DataType.BYTE_ARRAY)
    private byte[] data;


    public Image(long id, byte[] data) {
        ///!!! ONLY FOR LOCAL USE. DO NOT TRY THIS IN SERVER SOLUTIONS PLEASE!!!!
        this.id = Arrays.hashCode(data);
        this.data = data;
    }

    public Image() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        ///!!! ONLY FOR LOCAL USE. DO NOT TRY THIS IN SERVER SOLUTIONS PLEASE!!!!
        this.id = Arrays.hashCode(data);
        this.data = data;
    }


    @Override
    public boolean equals(Object obj) {
        try{
            if (this == obj) return true;
            if (!(obj instanceof Image other)) return false;
            return Arrays.equals(this.data, other.data);
        }catch (Exception e){
            return false;
        }
    }
    @Override
    public int hashCode() {
        return (int) id;
    }
}
