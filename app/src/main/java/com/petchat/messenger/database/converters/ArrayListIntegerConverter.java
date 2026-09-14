package com.petchat.messenger.database.converters;

import androidx.room.TypeConverter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class ArrayListIntegerConverter {
    @TypeConverter
    public static String fromArrayList(ArrayList<Integer> list) {
        if (list == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static ArrayList<Integer> toArrayList(String value) {
        if (value == null) {
            return null;
        }
        Type type = new TypeToken<ArrayList<Integer>>() {}.getType();
        return new Gson().fromJson(value, type);
    }
}
