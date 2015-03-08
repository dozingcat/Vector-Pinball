package com.dozingcatsoftware.bouncy;

public class GameState {

    // Defines how score multiplier is affected when ball is lost.
    public static enum MultiplierBehavior {
        REMOVE,  // Reset to 1.
        HOLD,    // Don't change.
        ROUND_HALF_DOWN,  // Reduce by half and round down, the default.
    }

    boolean gameInProgress = false;
    boolean paused = true;

    int ballNumber;
    int extraBalls;
    int totalBalls = 3;

    long score;
    double scoreMultiplier;
    MultiplierBehavior multiplierBehavior;

    public void startNewGame() {
        score = 0;
        ballNumber = 1;
        scoreMultiplier = 1;
        multiplierBehavior = MultiplierBehavior.ROUND_HALF_DOWN;

        gameInProgress = true;
        paused = false;
    }

    public void doNextBall() {
        switch (multiplierBehavior) {
        case REMOVE:
            scoreMultiplier = 1;
            break;
        case HOLD:
            break;
        case ROUND_HALF_DOWN:
            scoreMultiplier = Math.max(1, Math.floor(scoreMultiplier/2));
            break;
        }

        if (extraBalls>0) {
            --extraBalls;
        }
        else if (ballNumber < totalBalls) {
            ++ballNumber;
        }
        else {
            gameInProgress = false;
        }
    }

    public void addScore(long points) {
        score += points * scoreMultiplier;
    }

    public void addExtraBall() {
        ++extraBalls;
    }

    public void incrementScoreMultiplier() {
        scoreMultiplier += 1;
    }

    public boolean isGameInProgress() {
        return gameInProgress;
    }
    public void setGameInProgress(boolean value) {
        gameInProgress = value;
    }

    public boolean isPaused() {
        return paused;
    }
    public void setPaused(boolean value) {
        paused = value;
    }

    public int getBallNumber() {
        return ballNumber;
    }
    public void setBallNumber(int ballNumber) {
        this.ballNumber = ballNumber;
    }

    public int getExtraBalls() {
        return extraBalls;
    }
    public void setExtraBalls(int extraBalls) {
        this.extraBalls = extraBalls;
    }

    public int getTotalBalls() {
        return totalBalls;
    }
    public void setTotalBalls(int totalBalls) {
        this.totalBalls = totalBalls;
    }

    public long getScore() {
        return score;
    }
    public void setScore(long score) {
        this.score = score;
    }

    public double getScoreMultiplier() {
        return scoreMultiplier;
    }
    public void setScoreMultiplier(double scoreMultiplier) {
        this.scoreMultiplier = scoreMultiplier;
    }

    public MultiplierBehavior getMultiplierBehavior() {
        return multiplierBehavior;
    }
    public void setMultiplierBehavior(MultiplierBehavior behavior) {
        multiplierBehavior = behavior;
    }
}
