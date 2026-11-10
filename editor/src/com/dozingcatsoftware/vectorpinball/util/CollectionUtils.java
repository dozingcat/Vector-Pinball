package com.dozingcatsoftware.vectorpinball.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtils {

    // These methods don't support self-referential structures.

    public static <K, V> Map<K, V> mutableDeepCopyOfMap(Map<K, V> src) {
        Map<K, V> result = new HashMap<K, V>();
        for (K key : src.keySet()) {
            result.put(key, (V)deepCopyIfNeeded(src.get(key)));
        }
        return result;
    }

    public static <V> List<V> mutableDeepCopyOfList(List<V> src) {
        List<V> result = new ArrayList<V>();
        for (V val : src) {
            result.add((V)deepCopyIfNeeded(val));
        }
        return result;
    }

    static Object deepCopyIfNeeded(Object obj) {
        if (obj instanceof Map) {
            return mutableDeepCopyOfMap((Map)obj);
        }
        if (obj instanceof List) {
            return mutableDeepCopyOfList((List)obj);
        }
        return obj;
    }
}
