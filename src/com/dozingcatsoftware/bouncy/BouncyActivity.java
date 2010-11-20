package com.dozingcatsoftware.bouncy;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

public class BouncyActivity extends Activity {
	
	// libgdx 0.7 no longer loads the native library in World.java
	static {
		System.loadLibrary("gdx");
	}
	
	FieldView worldView;
	ScoreView scoreView;
	
	MenuItem aboutMenuItem;
	MenuItem endGameMenuItem;
	
	Handler handler = new Handler();
	
	Runnable callTick = new Runnable() {
		public void run() {tick();}
	};
	
	Field field = new Field();
	boolean running;
	int level = 1;
	long highScore = 0;
	static String HIGHSCORE_PREFS_KEY = "highScore";
	
	FieldDriver fieldDriver = new FieldDriver();
	OrientationListener orientationListener;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        field.resetForLevel(this, level);
        
        worldView = (FieldView)findViewById(R.id.worldView);
        worldView.setField(field);
        
        scoreView = (ScoreView)findViewById(R.id.scoreView);
        scoreView.setField(field);
        
        fieldDriver.setFieldView(worldView);
        fieldDriver.setField(field);
        
        worldView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				handleMainViewTouch(event);
				return true;
			}
    	});
        
        highScore = this.highScoreFromPreferences();
        scoreView.setHighScore(highScore);

        // TODO: allow field configuration to specify whether tilting is allowed
        /*
        orientationListener = new OrientationListener(this, SensorManager.SENSOR_DELAY_GAME,
        		new OrientationListener.Delegate() {
        	public void receivedOrientationValues(float azimuth, float pitch, float roll) {
            	field.receivedOrientationValues(azimuth, pitch, roll);
        	}
        });
        */
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	running = true;
        handler.postDelayed(callTick, 75);
        if (orientationListener!=null) orientationListener.start();
        
        fieldDriver.start();
    }
    
    @Override
    public void onPause() {
    	running = false;
    	if (orientationListener!=null) orientationListener.stop();
    	fieldDriver.stop();
    	super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	aboutMenuItem = menu.add(R.string.about_menu_item);
    	endGameMenuItem = menu.add(R.string.end_game_menu_item);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item==aboutMenuItem) {
    		Intent aboutIntent = new Intent(getBaseContext(), AboutActivity.class);
    		this.startActivity(aboutIntent);
    	}
    	else if (item==endGameMenuItem) {
    		field.endGame();
    	}
    	return true;
    }

    // called every 100 milliseconds while app is visible, to update score view and high score
    void tick() {
    	scoreView.invalidate();
    	checkForHighScore();
    	handler.postDelayed(callTick, 100);
    }
    
    /** If the score of the current or previous game is greater than the previous high score, update high score in 
     * preferences and ScoreView.
     */
    void checkForHighScore() {
    	synchronized(field) {
    		if (!field.hasActiveElements()) {
            	long score = field.getGameState().getScore();
            	if (score > this.highScore) {
            		this.updateHighScore(score);
            	}
    		}
    	}
    }
    
    /** Returns the high score stored in SharedPreferences, or 0 if no score is stored. */
    long highScoreFromPreferences() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	return prefs.getLong(HIGHSCORE_PREFS_KEY, 0);
    }
    
    /** Updates the highScore instance variable, the ScoreView display, and writes the score to SharedPreferences. */
    void updateHighScore(long score) {
    	this.highScore = score;
    	scoreView.setHighScore(score);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putLong(HIGHSCORE_PREFS_KEY, score);
    	editor.commit();
    }
    
    /** Called when the main view is touched. Activates flippers, starts a new game if one is not in progress, and
     * launches a ball if one is not in play.
     */
    void handleMainViewTouch(MotionEvent event) {
    	synchronized(field) {
        	if (event.getAction()==MotionEvent.ACTION_DOWN) {
        		// start game if not in progress
        		if (!field.getGameState().isGameInProgress()) {
        			field.resetForLevel(this, level);
        			field.startGame();
        		}
            	// remove "dead" balls and launch if none already in play
        		field.removeDeadBalls();
        		if (field.getBalls().size()==0) field.launchBall();
        	}
        	// activate or deactivate flippers
        	boolean flipperState = !(event.getAction()==MotionEvent.ACTION_UP);
        	field.setAllFlippersEngaged(flipperState);
    	}
    }
}