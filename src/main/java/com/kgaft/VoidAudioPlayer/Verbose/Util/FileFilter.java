package com.kgaft.VoidAudioPlayer.Verbose.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class FileFilter {
    public static HashMap<String, List<File>> filterFiles(File[] input, HashMap<String, List<String>> filters){
        HashMap<String, List<File>> result = new HashMap<>();
        for(File file : Objects.requireNonNull(input)){
            filters.forEach((key, value) -> {
                for(String valFilter: value){
                    if(file.isFile() && file.getName().endsWith(valFilter)){
                        List<File> target = result.get(key);
                        if(target == null){
                            target = new ArrayList<>();
                        }
                        target.add(file);
                        result.put(key, target);
                    }
                }
            });
        }
        return result;
    }
}
