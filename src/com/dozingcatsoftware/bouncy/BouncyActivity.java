package com.dozingcatsoftware.bouncy;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

public class BouncyActivity extends Activity {
	
	// libgdx 0.7 no longer loads the native library in World.java
	static {
		System.loadLibrary("gdx");
	}
	
	FieldView worldView;
	ScoreView scoreView;
	
	View buttonPanel;
	MenuItem aboutMenuItem;
	MenuItem endGameMenuItem;
	MenuItem preferencesMenuItem;
	final static int ACTIVITY_PREFERENCES = 1;
	
	Handler handler = new Handler();
	
	Runnable callTick = new Runnable() {
		public void run() {tick();}
	};
	
	Field field = new Field();
	boolean running;
	int level = 1;
	long highScore = 0;
	static String HIGHSCORE_PREFS_KEY = "highScore";
	boolean useZoom = true;
	
	FieldDriver fieldDriver = new FieldDriver();
	OrientationListener orientationListener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        FieldLayout.setContext(this);
        field.resetForLevel(this, level);
        
        worldView = (FieldView)findViewById(R.id.worldView);
        worldView.setField(field);
        
        scoreView = (ScoreView)findViewById(R.id.scoreView);
        scoreView.setField(field);
        
        fieldDriver.setFieldView(worldView);
        fieldDriver.setField(field);
        
        highScore = this.highScoreFromPreferences();
        scoreView.setHighScore(highScore);
        
        buttonPanel = findViewById(R.id.buttonPanel);

        // TODO: allow field configuration to specify whether tilting is allowed
        /*
        orientationListener = new OrientationListener(this, SensorManager.SENSOR_DELAY_GAME,
        		new OrientationListener.Delegate() {
        	public void receivedOrientationValues(float azimuth, float pitch, float roll) {
            	field.receivedOrientationValues(azimuth, pitch, roll);
        	}
        });
        */
        updateFromPreferences();
        // initialize audio
        VPSoundpool.initSounds(this);
        VPSoundpool.loadSounds();
        
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
    

    void gotoPreferences() {
		Intent settingsActivity = new Intent(getBaseContext(), BouncyPreferences.class);
		startActivityForResult(settingsActivity, ACTIVITY_PREFERENCES);
    }
    
    void gotoAbout() {
		Intent aboutIntent = new Intent(getBaseContext(), AboutActivity.class);
		this.startActivity(aboutIntent);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	running = true;
        handler.postDelayed(callTick, 75);
        if (orientationListener!=null) orientationListener.start();
        
        fieldDriver.start();
        worldView.onResume();
    }
    
    @Override
    public void onPause() {
    	running = false;
    	if (orientationListener!=null) orientationListener.stop();
    	fieldDriver.stop();
    	worldView.onPause();
    	super.onPause();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	aboutMenuItem = menu.add(R.string.about_menu_item);
    	endGameMenuItem = menu.add(R.string.end_game_menu_item);
    	preferencesMenuItem = menu.add(R.string.preferences_menu_item);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item==aboutMenuItem) {
    		gotoAbout();
    	}
    	else if (item==endGameMenuItem) {
    		field.endGame();
    	}
    	else if (item==preferencesMenuItem) {
    		gotoPreferences();
    	}
    	return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) { 
        super.onActivityResult(requestCode, resultCode, intent); 

        switch(requestCode) { 
            case ACTIVITY_PREFERENCES:
            	updateFromPreferences();
            	break;
        }
    }

    // Update settings from preferences, called at launch and when preferences activity finishes
    void updateFromPreferences() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	worldView.setIndependentFlippers(prefs.getBoolean("independentFlippers", false));
    	scoreView.setShowFPS(prefs.getBoolean("showFPS", false));

    	useZoom = prefs.getBoolean("zoom", true);
    	worldView.setZoom(useZoom ? 1.4f : 1.0f);
    	
    	VPSoundpool.setSoundEnabled(prefs.getBoolean("sound", true));
    }

    // called every 100 milliseconds while app is visible, to update score view and high score
    void tick() {
    	scoreView.invalidate();
    	scoreView.setFPS(fieldDriver.getAverageFPS());
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
    		if (!field.getGameState().isGameInProgress()) {
    			buttonPanel.setVisibility(View.VISIBLE);
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
    
    // button action methods (defined by android:onClick values in main.xml)
    public void doStartGame(View view) {
    	buttonPanel.setVisibility(View.GONE);
    	field.resetForLevel(this, level);
    	field.startGame();
    	VPSoundpool.playStart();
    }
    
    public void doPreferences(View view) {
    	gotoPreferences();
    }
    
    public void doAbout(View view) {
    	gotoAbout();
    }
    
    public void doSwitchTable(View view) {
    	level = (level==FieldLayout.numberOfLevels()) ? 1 : level+1;
    	field.resetForLevel(this, level);
    }
}