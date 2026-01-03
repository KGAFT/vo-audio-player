package com.kgaft.VoidAudioPlayer.Ui.StringUtils;

import java.util.*;

public class StringManager {
    private static ResourceBundle currentLocale = ResourceBundle.getBundle("messages", Locale.ENGLISH);

    public static String getString(String name) {
        return currentLocale.getString(name);
    }

    public static void switchLocale(Locale newLocale){
        currentLocale = ResourceBundle.getBundle("messages", newLocale);
    }
}
