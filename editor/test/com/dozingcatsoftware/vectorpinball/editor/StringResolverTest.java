package com.dozingcatsoftware.vectorpinball.editor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class StringResolverTest {

    static Map<String, String> parse(String xml) {
        return StringResolver.parseAndroidStrings(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void testUnescape() {
        assertEquals("plain", StringResolver.unescapeAndroidString("plain"));
        assertEquals("line1\nline2\ttab", StringResolver.unescapeAndroidString("line1\\nline2\\ttab"));
        assertEquals("it's \"quoted\" \\ @ ?",
                StringResolver.unescapeAndroidString("it\\'s \\\"quoted\\\" \\\\ \\@ \\?"));
        assertEquals("é", StringResolver.unescapeAndroidString("\\u00e9"));
        // Unescaped quotes are removed.
        assertEquals("a lucky card", StringResolver.unescapeAndroidString("a \"lucky\" card"));
        assertEquals("100%%", StringResolver.unescapeAndroidString("100%%\""));
        // Whitespace is collapsed and trimmed, except inside quotes.
        assertEquals("a b c", StringResolver.unescapeAndroidString("  a \n   b\tc  "));
        assertEquals("a  b c", StringResolver.unescapeAndroidString("\"a  b\"   c"));
    }

    @Test public void testParse() {
        Map<String, String> strings = parse(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n" +
                "  <string name=\"simple\">Simple</string>\n" +
                "  <string name=\"entity\">Rock &amp; roll &quot;stripped&quot;</string>\n" +
                "  <string name=\"xliff\">Ball <xliff:g id=\"n\">%1$d</xliff:g></string>\n" +
                "  <string-array name=\"array\"><item>Ignored</item></string-array>\n" +
                "</resources>\n");
        assertEquals(3, strings.size());
        assertEquals("Simple", strings.get("simple"));
        assertEquals("Rock & roll stripped", strings.get("entity"));
        assertEquals("Ball %1$d", strings.get("xliff"));
    }

    @Test public void testResolveAppStrings() {
        StringResolver resolver = new StringResolver();
        assertEquals("Ball 2", resolver.resolveString("ball_number_message", 2));
        assertEquals("Bumper Multiplier 5%", resolver.resolveString("bumper_multiplier_message", 5));
        assertEquals("Full House! +12000",
                resolver.resolveString("poker_hand_scored_message", "Full House", 12000L));
        assertEquals("UNKNOWN KEY: no_such_key", resolver.resolveString("no_such_key"));
        assertTrue(resolver.resolveString("table9_rules").contains("a \"lucky\" card"));
    }

    /**
     * Checks that every message key in the core code is defined in the app's strings.xml. Keys
     * are found by looking for string literals ending in "_message" or passed to resolveString.
     */
    @Test public void testAllCoreMessageKeysAreDefined() throws IOException {
        Path coreDir = Paths.get(System.getProperty("vectorpinball.coreSourceDir"));
        Pattern keyPattern = Pattern.compile(
                "\"([a-z0-9_]+_message)\"|resolveString\\(\\s*\"([a-z0-9_]+)\"");
        Set<String> keys = new TreeSet<>();
        try (Stream<Path> files = Files.walk(coreDir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java"))
                    .collect(Collectors.toList())) {
                Matcher m = keyPattern.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
                while (m.find()) {
                    keys.add(m.group(1) != null ? m.group(1) : m.group(2));
                }
            }
        }
        // Sanity check that the scan found the keys.
        assertTrue(keys.size() > 20);
        StringResolver resolver = new StringResolver();
        Set<String> missing = keys.stream()
                .filter(k -> !resolver.hasString(k))
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals("Keys used in core but missing from strings.xml", new TreeSet<>(), missing);
    }
}
