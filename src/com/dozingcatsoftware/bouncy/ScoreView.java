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
	}
	
	@Override
	public void draw(Canvas c) {
		String displayString = null;
		synchronized(field) {
			// show custom message if present
			GameMessage msg = field.getGameMessage();
			displayString = (msg!=null) ? msg.text : null;
			if (displayString==null) {
				// show score if game is in progress, otherwise cycle between "Touch to start"/previous score/high score
				if (field.getGameState().isGameInProgress()) {
					displayString = SCORE_FORMAT.format(field.getGameState().getScore());
				}
				else {
					boolean cycle = false;
					if (lastUpdateTime==null) {
						lastUpdateTime = System.currentTimeMillis();
					}
					else if (System.currentTimeMillis() - lastUpdateTime > gameOverMessageCycleTime) {
						cycle = true;
						lastUpdateTime = System.currentTimeMillis();
					}
					displayString = displayedGameOverMessage(cycle);
				}
			}
		}
		
		textPaint.getTextBounds(displayString, 0, displayString.length(), textRect);
		// textRect ends up being too high
		c.drawText(displayString, this.getWidth()/2 - textRect.width()/2, this.getHeight()/2, textPaint);
		if (showFPS && fps>0) {
			c.drawText(String.format("%.1f fps", fps), 0, 20, fpsPaint);
		}
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
