package com.dozingcatsoftware.bouncy;

import java.text.NumberFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

/**
 * This class displays the score and game messages above the game view. When there is no game in progress, it cycles
 * between a "Touch to Start" message, last score, and high scores.
 */

public class ScoreView extends View {

    Field field;
    Paint textPaint = new Paint();
    Rect textRect = new Rect();

    Paint fpsPaint = new Paint();
    Rect fpsRect = new Rect();

    Paint usedBallPaint = new Paint();
    Paint remainingBallPaint = new Paint();
    Paint multiplierPaint = new Paint();

    List<Long> highScores;
    Long lastUpdateTime;

    static final int TOUCH_TO_START_MESSAGE = 0;
    static final int LAST_SCORE_MESSAGE = 1;
    static final int HIGH_SCORE_MESSAGE = 2;

    int gameOverMessageIndex = TOUCH_TO_START_MESSAGE;
    int highScoreIndex = 0;
    int gameOverMessageCycleTime = 3500;

    double fps;
    boolean showFPS = false;

    static NumberFormat SCORE_FORMAT = NumberFormat.getInstance();

    public ScoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setARGB(255, 255, 255, 0);
        textPaint.setAntiAlias(true);
        // setTextSize uses absolute pixels, get screen density to scale.
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager =
                (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        textPaint.setTextSize(24 * metrics.density);

        fpsPaint.setARGB(255, 255, 255, 0);
        fpsPaint.setTextSize(6 * metrics.density);

        multiplierPaint.setARGB(255, 32, 224, 32);
        multiplierPaint.setTextSize(12 * metrics.density);

        usedBallPaint.setARGB(255, 128, 128, 128);
        usedBallPaint.setStyle(Paint.Style.STROKE);
        remainingBallPaint.setARGB(255, 224, 224, 224);
        remainingBallPaint.setStyle(Paint.Style.FILL);
    }

    @Override public void draw(Canvas c) {
        GameMessage msg = null;
        boolean gameInProgress = false;
        boolean ballInPlay = false;
        int totalBalls = 0;
        int currentBall = 0;
        double multiplier = 0;
        long score = 0;
        synchronized(field) {
            // Show custom message if present.
            msg = field.getGameMessage();
            GameState state = field.getGameState();
            gameInProgress = state.isGameInProgress();
            totalBalls = state.getTotalBalls();
            currentBall = state.getBallNumber();
            multiplier = state.getScoreMultiplier();
            score = (gameInProgress) ? state.getScore() : 0;
            ballInPlay = field.getBalls().size() > 0;
        }

        c.drawARGB(255, 8, 8, 8);
        String displayString = (msg!=null) ? msg.text : null;
        if (displayString==null) {
            // Show score if game is in progress, otherwise cycle between
            // "Touch to start"/previous score/high score.
            if (gameInProgress) {
                displayString = SCORE_FORMAT.format(score);
            }
            else {
                long now = currentMillis();
                if (lastUpdateTime==null) {
                    lastUpdateTime = now;
                }
                else if (now - lastUpdateTime > gameOverMessageCycleTime) {
                    cycleGameOverMessage();
                    lastUpdateTime = now;
                }
                displayString = displayedGameOverMessage();
            }
        }

        int width = this.getWidth();
        int height = this.getHeight();
        textPaint.getTextBounds(displayString, 0, displayString.length(), textRect);
        // textRect ends up being too high
        c.drawText(displayString, width/2 - textRect.width()/2, height/2 + textRect.height()/2,
                textPaint);
        if (showFPS && fps>0) {
            c.drawText(String.format("%.1f fps", fps), width * 0.02f, height * 0.25f, fpsPaint);
        }
        if (gameInProgress) {
            // Draw balls.
            float ballRadius = width / 75f;
            float ballOuterMargin = 2 * ballRadius;
            float ballBetweenSpace = ballRadius;
            float ballCenterY = height - (ballOuterMargin + ballRadius);
            float ballCenterX = 0;
            for (int i=0; i<totalBalls; i++) {
                ballCenterX = width - ballOuterMargin - ballRadius -
                        (i * (2*ballRadius + ballBetweenSpace));
                // "Remove" ball from display when launched.
                boolean isRemaining = (currentBall + i + (ballInPlay ? 1 : 0) <= totalBalls);
                c.drawCircle(ballCenterX, ballCenterY, ballRadius,
                        isRemaining ? remainingBallPaint : usedBallPaint);
            }
            // Draw multiplier if >1. Use X position of leftmost ball.
            if (multiplier > 1) {
                int intValue = (int) multiplier;
                String multiplierString = (multiplier == intValue) ?
                        intValue + "x" : String.format("%.2fx", multiplier);
                c.drawText(multiplierString, ballCenterX-ballRadius, height * 0.4f,
                        multiplierPaint);
            }
        }
    }

    long currentMillis() {
        return System.currentTimeMillis();
    }

    // Cycles to the next message to show when there is not game in progress. This can be
    // "Touch to start", the last score if available, or one of the previous high scores.
    void cycleGameOverMessage() {
        switch (gameOverMessageIndex) {
            case TOUCH_TO_START_MESSAGE:
                if (field.getScore() > 0) {
                    gameOverMessageIndex = LAST_SCORE_MESSAGE;
                }
                else if (highScores.get(0) > 0) {
                    gameOverMessageIndex = HIGH_SCORE_MESSAGE;
                    highScoreIndex = 0;
                }
                break;
            case LAST_SCORE_MESSAGE:
                if (highScores.get(0) > 0) {
                    gameOverMessageIndex = HIGH_SCORE_MESSAGE;
                    highScoreIndex = 0;
                }
                break;
            case HIGH_SCORE_MESSAGE:
                highScoreIndex++;
                if (highScoreIndex >= highScores.size() || highScores.get(highScoreIndex)<=0) {
                    highScoreIndex = 0;
                    gameOverMessageIndex = TOUCH_TO_START_MESSAGE;
                }
                break;
            default:
                throw new IllegalStateException("Unknown gameOverMessageIndex: " + gameOverMessageIndex);
        }
    }

    // Returns message to show when game is not in progress.
    String displayedGameOverMessage() {
        switch (gameOverMessageIndex) {
            case TOUCH_TO_START_MESSAGE:
                return "Touch to start";
            case LAST_SCORE_MESSAGE:
                return "Last Score: " + SCORE_FORMAT.format(field.getScore());
            case HIGH_SCORE_MESSAGE:
                // highScoreIndex could be too high if we just switched from a different table.
                int index = Math.min(highScoreIndex, this.highScores.size() - 1);
                String formattedScore = SCORE_FORMAT.format(this.highScores.get(index));
                if (index == 0) {
                    return "High Score: " + formattedScore;
                }
                else {
                    return "#" + (1+index) + " Score: " + formattedScore;
                }
            default:
                throw new IllegalStateException("Unknown gameOverMessageIndex: " + gameOverMessageIndex);
        }
    }

    public void setHighQuality(boolean highQuality) {
        int ballWidth = highQuality ? 2 : 0;
        remainingBallPaint.setStrokeWidth(ballWidth);
        usedBallPaint.setStrokeWidth(ballWidth);
    }

    public void setField(Field value) {
        field = value;
    }
    public void setHighScores(List<Long> value) {
        highScores = value;
    }
    public void setFPS(double value) {
        fps = value;
    }
    public void setShowFPS(boolean value) {
        showFPS = value;
    }
}
