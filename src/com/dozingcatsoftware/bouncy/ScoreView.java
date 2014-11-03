package com.dozingcatsoftware.bouncy;

import java.text.NumberFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

/** This class displays the score and game messages above the game view. When there is no game in progress, it cycles
 * between a "Touch to Start" message, last score, and high score.
 * @author brian
 *
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
	
	long highScore;
	Long lastUpdateTime;
	
	int gameOverMessageIndex = 0; // 0: "Touch to start", 1: last score, 2: high score
	int gameOverMessageCycleTime = 3500;
	
	double fps;
	boolean showFPS = false;
	
	static NumberFormat SCORE_FORMAT = NumberFormat.getInstance(); 

	public ScoreView(Context context, AttributeSet attrs) {
		super(context, attrs);
		textPaint.setARGB(255, 255, 255, 0);
		textPaint.setAntiAlias(true);
		// setTextSize uses absolute pixels, get screen density to scale
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
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
	
	@Override
	public void draw(Canvas c) {
		GameMessage msg = null;
		boolean gameInProgress = false;
		boolean ballInPlay = false;
		int totalBalls = 0;
		int currentBall = 0;
		int multiplier = 0;
		long score = 0;
		synchronized(field) {
			// show custom message if present
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
			// show score if game is in progress, otherwise cycle between
		    // "Touch to start"/previous score/high score
			if (gameInProgress) {
				displayString = SCORE_FORMAT.format(score);
			}
			else {
				boolean cycle = false;
				long now = currentMillis();
				if (lastUpdateTime==null) {
					lastUpdateTime = now;
				}
				else if (now - lastUpdateTime > gameOverMessageCycleTime) {
					cycle = true;
					lastUpdateTime = now;
				}
				displayString = displayedGameOverMessage(cycle);
			}
		}

		int width = this.getWidth();
		int height = this.getHeight();
		textPaint.getTextBounds(displayString, 0, displayString.length(), textRect);
		// textRect ends up being too high
		c.drawText(displayString, width/2 - textRect.width()/2, height/2 + textRect.height()/2, textPaint);
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
		        ballCenterX = width - ballOuterMargin - ballRadius - (i * (2*ballRadius + ballBetweenSpace));
		        // "Remove" ball from display when launched.
		        boolean isRemaining = (currentBall + i + (ballInPlay ? 1 : 0) <= totalBalls);
		        c.drawCircle(ballCenterX, ballCenterY, ballRadius,
		                isRemaining ? remainingBallPaint : usedBallPaint);
		    }
		    // Draw multiplier if >1. Use X position of leftmost ball.
		    if (multiplier > 1) {
		        c.drawText(multiplier + "x", ballCenterX-ballRadius, height * 0.4f, multiplierPaint);
		    }
		}
	}

	long currentMillis() {
	    return System.currentTimeMillis();
	}
	
	// Returns message to show when game is not in progress. Can be "Touch to start", high score, or previous score.
	// If cycle parameter is true, moves to next message if possible.
	String displayedGameOverMessage(boolean cycle) {
		String msg = null;
		if (cycle) {
			gameOverMessageIndex = (gameOverMessageIndex+1) % 3;
		}
		while (msg==null) {
			switch(gameOverMessageIndex) {
			case 0:
				return "Touch to start";
			case 1:
				long score = field.getGameState().getScore();
				if (score > 0) return "Last Score: " + SCORE_FORMAT.format(score);
				break;
			case 2:
				if (this.highScore > 0) return "High Score: " + SCORE_FORMAT.format(this.highScore);
				break;
			}
			gameOverMessageIndex = (gameOverMessageIndex+1) % 3;
		}
		return msg;
	}

	public void setHighQuality(boolean highQuality) {
	    int ballWidth = highQuality ? 2 : 0;
	    remainingBallPaint.setStrokeWidth(ballWidth);
	    usedBallPaint.setStrokeWidth(ballWidth);
	}
	
	public void setField(Field value) {
		field = value;
	}
	public void setHighScore(long value) {
		highScore = value;
	}
	public void setFPS(double value) {
		fps = value;
	}
	public void setShowFPS(boolean value) {
		showFPS = value;
	}
}
