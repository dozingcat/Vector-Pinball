package com.dozingcatsoftware.bouncy;

public interface AudioPlayer {

    void playStart();

    void playBall();

    void playFlipper();

    void playScore();

    void playMessage();

    void playRollover();

    public static class NoOpPlayer implements AudioPlayer {
        private static final NoOpPlayer INSTANCE = new NoOpPlayer();

        public static NoOpPlayer getInstance() {
            return INSTANCE;
        }

        private NoOpPlayer() {}
        
        @Override public void playStart() {}
        @Override public void playBall() {}
        @Override public void playFlipper() {}
        @Override public void playScore() {}
        @Override public void playMessage() {}
        @Override public void playRollover() {}
    }
}
