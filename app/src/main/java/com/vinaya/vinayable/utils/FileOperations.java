package com.vinaya.vinayable.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Operations with SharedPreferences
 */
public class FileOperations {

    /**
     * Update records
     * @param context
     * @param data new data to save
     * @param mac address of the device which send the new data
     * @param tx tx if is true, rx if is false
     */
    public static void saveLatestCommunication(Context context, String data, String mac, boolean tx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String baseKey;
        if (tx)
            baseKey = "TX" + mac;
        else
            baseKey = "RX" + mac;
        String saveData1 = prefs.getString(baseKey + "1", "");
        String saveData0 = prefs.getString(baseKey + "0", "");

        editor.putString(baseKey + "2", saveData1);
        editor.putString(baseKey + "1", saveData0);
        editor.putString(baseKey + "0", data);
        editor.commit();
    }

    /**
     * Read records
     * @param context
     * @param mac address of the device which we want read the records
     * @param tx tx if is true, rx if is false
     * @return
     */
    public static List<String> readLatestCommunication(Context context, String mac, boolean tx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<String> stringList = new ArrayList<>();
        String baseKey;
        if (tx)
            baseKey = "TX" + mac;
        else
            baseKey = "RX" + mac;

        stringList.add(prefs.getString(baseKey + "0", ""));
        stringList.add(prefs.getString(baseKey + "1", ""));
        stringList.add(prefs.getString(baseKey + "2", ""));
        return stringList;
    }
}
