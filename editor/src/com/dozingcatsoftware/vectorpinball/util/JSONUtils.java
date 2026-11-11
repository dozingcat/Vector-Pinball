package com.dozingcatsoftware.vectorpinball.util;

import java.util.List;
import java.util.Map;

import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

/**
 * Utility methods for converting JSON to lists and maps.
 * Depends on simplejson: https://code.google.com/p/json-simple/
 */
public class JSONUtils {

    public static class ParsingException extends RuntimeException {
        ParsingException(Throwable cause) {
            super(cause);
        }
    }

	/** Returns a List created by parsing the string argument as a JSON array and calling listFromJSONArray.
	 */
	public static List<Object> listFromJSONString(String jsonString) {
	    try {
    	    return (List) JSONValue.parseWithException(jsonString);
	    }
	    catch (ParseException pex) {
	        throw new ParsingException(pex);
	    }
	}

	/** Returns a Map created by parsing the string argument as a JSON object and calling mapFromJSONObject.
	 */
	public static Map<String, Object> mapFromJSONString(String jsonString) {
        try {
            return (Map) JSONValue.parseWithException(jsonString);
        }
        catch (ParseException pex) {
            throw new ParsingException(pex);
        }
	}

	public static String jsonStringFromObject(Object obj) {
	    //return JSONValue.toJSONString(obj);
	    return JsonPrettyPrinter.prettyPrint(obj, 4);
	}
}
