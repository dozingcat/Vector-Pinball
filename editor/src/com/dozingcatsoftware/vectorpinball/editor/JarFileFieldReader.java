package com.dozingcatsoftware.vectorpinball.editor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import com.dozingcatsoftware.vectorpinball.util.JSONUtils;

public class JarFileFieldReader {

    // Built-in tables are shared with the Android app (from shared-assets/tables).
    private static final String BUILT_IN_TABLE_PATH = "/tables/";
    // Editor-only tables (from editor/resources/fields).
    private static final String EDITOR_FIELD_PATH = "/fields/";

    String pathForBuiltInField(int level) {
        return BUILT_IN_TABLE_PATH + "table" + level + ".json";
    }

    String pathForStarterField() {
        return EDITOR_FIELD_PATH + "starter.json";
    }

    InputStream inputStreamForStarterField(int level) {
        return getClass().getResourceAsStream(pathForStarterField());
    }

    public Map<String, Object> layoutMapForLevel(String resourcePath) {
        try(InputStream fin = getClass().getResourceAsStream(resourcePath)) {
            if (fin==null) {
                throw new IllegalArgumentException("Field at " + resourcePath + " not found");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(fin));

            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line=br.readLine())!=null) {
                buffer.append(line);
            }
            return JSONUtils.mapFromJSONString(buffer.toString());
        }
        catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Map<String, Object> layoutMapForBuiltInField(int fieldNum) {
        return layoutMapForLevel(pathForBuiltInField(fieldNum));
    }

    public Map<String, Object> layoutMapForStarterField() {
        return layoutMapForLevel(pathForStarterField());
    }
}
