package com.dozingcatsoftware.bouncy;

/**
 * Interface to look up localized strings. Allows Field and delegate classes to generate localized
 * messages without platform-specific code.
 */
public interface IStringResolver {
    // It's slightly annoying that `key` doesn't get checked at compile time, as compared to
    // Android's autogenerated R.string.foo. But even on Android the parameters aren't checked.
    String resolveString(String key, Object... params);
}
