package com.kgaft.VoidAudioPlayer.Util;

import java.io.*;
import java.util.HashMap;

public class HashMapSer {

        public static <K extends Serializable, V extends Serializable> byte[] serialize(HashMap<K, V> map)
                throws IOException {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                oos.writeObject(map);
                return bos.toByteArray();
            }
        }

        @SuppressWarnings("unchecked")
        public static <K extends Serializable, V extends Serializable> HashMap<K, V> deserialize(byte[] data)
                throws IOException, ClassNotFoundException {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
                 ObjectInputStream ois = new ObjectInputStream(bis)) {
                return (HashMap<K, V>) ois.readObject();
            }
        }

}
