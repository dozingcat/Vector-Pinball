package com.dozingcatsoftware.bouncy;

/**
 * Interface to look up localized strings. Allows Field and delegate classes to generate localized
 * messages without platform-specific code.
 */
public interface IStringResolver {
    public String resolveString(String key, Object... params);
}
