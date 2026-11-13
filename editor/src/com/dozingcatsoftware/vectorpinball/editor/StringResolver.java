package com.dozingcatsoftware.vectorpinball.editor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.dozingcatsoftware.vectorpinball.model.IStringResolver;

/**
 * Resolves game message strings using the Android app's string resources, so the editor shows the
 * same messages as the app. The build copies app/src/main/res/values/strings.xml into the editor's
 * resources at STRINGS_RESOURCE_PATH. Only English is supported for now.
 */
public class StringResolver implements IStringResolver {

    static final String STRINGS_RESOURCE_PATH = "/strings/strings.xml";

    private final Map<String, String> strings;

    public StringResolver() {
        this(loadStringsResource());
    }

    StringResolver(Map<String, String> strings) {
        this.strings = strings;
    }

    private static Map<String, String> loadStringsResource() {
        try (InputStream input = StringResolver.class.getResourceAsStream(STRINGS_RESOURCE_PATH)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + STRINGS_RESOURCE_PATH);
            }
            return parseAndroidStrings(input);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Same behavior as Android's Resources.getString(id, formatArgs), which the app uses: the
     * string is a java.util.Formatter pattern.
     */
    @Override public String resolveString(String key, Object... params) {
        String value = strings.get(key);
        if (value == null) {
            return "UNKNOWN KEY: " + key;
        }
        return String.format(Locale.US, value, params);
    }

    public boolean hasString(String key) {
        return strings.containsKey(key);
    }

    /** Returns a map of names to values for the <string> elements in an Android resource file. */
    static Map<String, String> parseAndroidStrings(InputStream input) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // The file is trusted, but there's no reason to allow external entities.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            NodeList nodes = factory.newDocumentBuilder().parse(input).getElementsByTagName("string");
            Map<String, String> result = new HashMap<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                Element elem = (Element) nodes.item(i);
                // getTextContent includes the text of child elements like <xliff:g>.
                result.put(elem.getAttribute("name"), unescapeAndroidString(elem.getTextContent()));
            }
            return result;
        }
        catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Converts the text of a string resource to the value Android uses, following aapt2's rules:
     * - Backslash escapes: \n, \t, \', \", \\, \@, \?, and \\uXXXX. Other escaped characters are
     *   kept without the backslash.
     * - Unescaped double quotes are removed. Text inside them is kept as is.
     * - Outside double quotes, runs of whitespace (including newlines) become a single space, and
     *   leading and trailing whitespace is removed.
     * Note that XML entities like &amp;quot; are decoded by the XML parser before this, so they're
     * treated like the unescaped character.
     */
    static String unescapeAndroidString(String text) {
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        boolean pendingSpace = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (!inQuotes && Character.isWhitespace(c)) {
                pendingSpace = true;
                i++;
                continue;
            }
            if (pendingSpace) {
                if (sb.length() > 0) sb.append(' ');
                pendingSpace = false;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                i++;
            }
            else if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                i += 2;
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 <= text.length()) {
                            sb.append((char) Integer.parseInt(text.substring(i, i + 4), 16));
                            i += 4;
                        }
                        break;
                    default: sb.append(next); break;
                }
            }
            else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
