package com.dozingcatsoftware.bouncy;

import android.content.Context;

import com.dozingcatsoftware.vectorpinball.util.IOUtils;
import com.dozingcatsoftware.bouncy.util.JSONUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FieldLayoutReader {
    static Map<Integer, Map<String, Object>> _layoutMap = new HashMap<>();

    public static int getNumberOfLevels(Context context) {
        try {
            return (int) Arrays.stream(context.getAssets().list("tables"))
                    .filter(name -> name.matches("^table/d+/.json"))
                    .count();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static Map<String, Object> readFieldLayout(Context context, int level) {
        try {
            String assetPath = "tables/table" + level + ".json";
            InputStream fin = context.getAssets().open(assetPath);
            String s = IOUtils.utf8FromStream(fin);
            return JSONUtils.mapFromJSONString(s);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Map<String, Object> layoutMapForLevel(Context context, int level) {
        Map<String, Object> levelLayout = _layoutMap.get(level);
        if (levelLayout == null) {
            levelLayout = readFieldLayout(context, level);
            _layoutMap.put(level, levelLayout);
        }
        return levelLayout;
    }
}
