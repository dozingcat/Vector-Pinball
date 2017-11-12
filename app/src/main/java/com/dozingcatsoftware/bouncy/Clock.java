package com.dozingcatsoftware.bouncy;

public interface Clock {

    long currentTimeMillis();

    long nanoTime();

    public static class SystemClock implements Clock {
        private static SystemClock INSTANCE = new SystemClock();

        public static SystemClock getInstance() {
            return INSTANCE;
        }

        private SystemClock() {}

        @Override public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override public long nanoTime() {
            return System.nanoTime();
        }
    }
}
