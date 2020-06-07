package com.dozingcatsoftware.vectorpinball.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtils {
    public static String utf8FromStream(InputStream in) {
        int bufSize = 1024;
        byte[] buffer = new byte[bufSize];
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = in.read(buffer, 0, buffer.length)) > 0) {
                bos.write(buffer, 0, bytesRead);
            }
            return new String(bos.toByteArray(), "utf8");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
