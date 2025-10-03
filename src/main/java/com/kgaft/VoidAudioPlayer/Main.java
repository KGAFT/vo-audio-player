package com.kgaft.VoidAudioPlayer;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.kgaft.VoidAudioPlayer.Native.LibrarySerializer;

import com.kgaft.VoidAudioPlayer.Verbose.*;


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





    public static void main(String[] args) {
        LibraryIndex index = LibraryIndex.getInstance();
         index.addDirectory("C:/Users/kftg/Music/Test");
        try {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } catch (Exception e) {
            e.printStackTrace();
        }
        PlayerWindow window = new PlayerWindow(index.getAlbums());

    }
}