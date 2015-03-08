package com.dozingcatsoftware.bouncy.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {

    /**
     * If argument is a JSONArray or JSONObject, returns the equivalent List or Map. If argument
     * is JSONObject.NULL, returns null. Otherwise, returns the argument unchanged.
     */
    public static Object objectFromJSONItem(Object jsonItem) {
        if (jsonItem==JSONObject.NULL){
            return null;
        }
        if (jsonItem instanceof JSONArray) {
            return listFromJSONArray((JSONArray)jsonItem);
        }
        if (jsonItem instanceof JSONObject) {
            return mapFromJSONObject((JSONObject)jsonItem);
        }
        return jsonItem;
    }

    /**
     * Returns a List with the same objects in the same order as jsonArray. Recursively converts
     * nested JSONArray and JSONObject values to List and Map objects.
     */
    public static List<Object> listFromJSONArray(JSONArray jsonArray) {
        List<Object> result = new ArrayList<Object>();
        try {
            for(int i=0; i<jsonArray.length(); i++) {
                Object obj = objectFromJSONItem(jsonArray.get(i));
                result.add(obj);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    /**
     * Returns a List with the same keys and values as jsonObject. Recursively converts nested
     * JSONArray and JSONObject values to List and Map objects.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapFromJSONObject(JSONObject jsonObject) {
        Map<String, Object> result = new HashMap<String, Object>();
        try {
            for(Iterator<String> ki = jsonObject.keys(); ki.hasNext(); ) {
                String key = ki.next();
                Object value = objectFromJSONItem(jsonObject.get(key));
                result.put(key, value);
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }

    /** Parses the string argument as a JSON array and converts to a List. */
    public static List<Object> listFromJSONString(String jsonString) {
        try {
            return listFromJSONArray(new JSONArray(jsonString));
        }
        catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Parses the string argument as a JSON object and converts to a Map. */
    public static Map<String, Object> mapFromJSONString(String jsonString) {
        try {
            return mapFromJSONObject(new JSONObject(jsonString));
        }
        catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}
