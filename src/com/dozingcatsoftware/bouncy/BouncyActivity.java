package com.dozingcatsoftware.bouncy;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class BouncyActivity extends Activity {
	
	// libgdx 0.7 no longer loads the native library in World.java
	static {
		System.loadLibrary("gdx");
	}
	
	CanvasFieldView canvasFieldView;
	GLFieldView glFieldView;
	ScoreView scoreView;
	
	View buttonPanel;
	Button switchTableButton;
	Button endGameButton;
	final static int ACTIVITY_PREFERENCES = 1;
	
	Handler handler = new Handler();
	
	Runnable callTick = new Runnable() {
		public void run() {tick();}
	};
	
	Field field = new Field();
	int level = 1;
	long highScore = 0;
	static String HIGHSCORE_PREFS_KEY = "highScore";
	static String INITIAL_LEVEL_PREFS_KEY = "initialLevel";
	boolean useZoom = true;
	
	static long END_GAME_DELAY = 1000; // delay after ending a game, before a touch will start a new game
	Long endGameTime = System.currentTimeMillis() - END_GAME_DELAY;
	
	FieldDriver fieldDriver = new FieldDriver();
	FieldViewManager fieldViewManager = new FieldViewManager();
	OrientationListener orientationListener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        
        FieldLayout.setContext(this);
        this.level = getInitialLevel();
        field.resetForLevel(this, level);
        
        canvasFieldView = (CanvasFieldView)findViewById(R.id.canvasFieldView);
        glFieldView = (GLFieldView)findViewById(R.id.glFieldView);
        
        fieldViewManager.setField(field);
        fieldViewManager.setStartGameAction(new Runnable() {public void run() {doStartGame(null);}});
        
        scoreView = (ScoreView)findViewById(R.id.scoreView);
        scoreView.setField(field);
        
        fieldDriver.setFieldViewManager(fieldViewManager);
        fieldDriver.setField(field);
        
        highScore = this.highScoreFromPreferencesForCurrentLevel();
        scoreView.setHighScore(highScore);
        
        buttonPanel = findViewById(R.id.buttonPanel);
        switchTableButton = (Button)findViewById(R.id.switchTableButton);
        endGameButton = (Button)findViewById(R.id.endGameButton);

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

    @Override public void onResume() {
        super.onResume();
        // Attempt to call setSystemUiVisibility(1) which is "low profile" mode.
        try {
            Method setUiMethod = View.class.getMethod("setSystemUiVisibility", int.class);
            setUiMethod.invoke(scoreView, 1);
        }
        catch (Exception ignored) {}
    }
    

    void gotoPreferences() {
		Intent settingsActivity = new Intent(getBaseContext(), BouncyPreferences.class);
		startActivityForResult(settingsActivity, ACTIVITY_PREFERENCES);
    }
    
    void gotoAbout() {
    	AboutActivity.startForLevel(this, this.level);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
    	// this handles the main activity pausing and resuming, as well as the Android menu appearing and disappearing
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            pauseGame();
        } 
        else {
            // If game is in progress, we want to return to the paused menu rather than immediately
            // resuming. We need to draw the current field, which currently doesn't work reliably
            // for OpenGL views. For now the game will resume immediately when using OpenGL.
            if (field.getGameState().isGameInProgress() && glFieldView.getVisibility()==View.GONE) {
                fieldDriver.drawField();
                showPausedButtons();
            }
            else {
                unpauseGame();
            }
        }
    }

    public void pauseGame() {
        if (field.getGameState().isPaused()) return;
        field.getGameState().setPaused(true);

        if (orientationListener != null) orientationListener.stop();
        fieldDriver.stop();
        if (glFieldView != null) glFieldView.onPause();

        VPSoundpool.pauseMusic();
    }

    public void unpauseGame() {
        if (!field.getGameState().isPaused()) return;
        field.getGameState().setPaused(false);

        handler.postDelayed(callTick, 75);
        if (orientationListener != null) orientationListener.start();

        fieldDriver.start();
        if (glFieldView != null) glFieldView.onResume();

        if (field.getGameState().isGameInProgress()) {
            buttonPanel.setVisibility(View.GONE);
        }
    }

    void showPausedButtons() {
        endGameButton.setVisibility(View.VISIBLE);
        switchTableButton.setVisibility(View.GONE);
        buttonPanel.setVisibility(View.VISIBLE);
    }
    
    @Override
    public void onDestroy() {
        VPSoundpool.cleanup();
    	super.onDestroy();
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
    	fieldViewManager.setIndependentFlippers(prefs.getBoolean("independentFlippers", false));
    	scoreView.setShowFPS(prefs.getBoolean("showFPS", false));
    	
    	// if switching quality modes or OpenGL/Canvas, reset frame rate manager because maximum achievable frame rate may change
    	boolean highQuality = prefs.getBoolean("highQuality", false);
    	boolean previousHighQuality = fieldViewManager.isHighQuality();
    	fieldViewManager.setHighQuality(highQuality);
    	if (previousHighQuality!=fieldViewManager.isHighQuality()) {
    		fieldDriver.resetFrameRate();
    	}
    	scoreView.setHighQuality(highQuality);
    	
    	boolean useOpenGL = prefs.getBoolean("useOpenGL", false);
    	if (useOpenGL) {
    		if (glFieldView.getVisibility()!=View.VISIBLE) {
        		canvasFieldView.setVisibility(View.GONE);
                glFieldView.setVisibility(View.VISIBLE);
                fieldViewManager.setFieldView(glFieldView);
                fieldDriver.resetFrameRate();
    		}
    	}
    	else {
    		if (canvasFieldView.getVisibility()!=View.VISIBLE) {
        		glFieldView.setVisibility(View.GONE);
        		canvasFieldView.setVisibility(View.VISIBLE);
                fieldViewManager.setFieldView(canvasFieldView);
                fieldDriver.resetFrameRate();
    		}
    	}

    	useZoom = prefs.getBoolean("zoom", true);
    	fieldViewManager.setZoom(useZoom ? 1.4f : 1.0f);
    	
    	VPSoundpool.setSoundEnabled(prefs.getBoolean("sound", true));
    	VPSoundpool.setMusicEnabled(prefs.getBoolean("music", true));
    }

    // called every 100 milliseconds while app is visible, to update score view and high score
    void tick() {
    	scoreView.invalidate();
    	scoreView.setFPS(fieldDriver.getAverageFPS());
    	updateHighScoreAndButtonPanel();
    	handler.postDelayed(callTick, 100);
    }
    
    /** If the score of the current or previous game is greater than the previous high score, update high score in 
     * preferences and ScoreView. Also show button panel if game has ended.
     */
    void updateHighScoreAndButtonPanel() {
    	// we only need to check the first time the game is over, when the button panel isn't visible
    	if (buttonPanel.getVisibility()==View.VISIBLE) return;
    	synchronized(field) {
    		if (!field.getGameState().isGameInProgress()) {
    			// game just ended, show button panel and set end game timestamp
    			this.endGameTime = System.currentTimeMillis();
    			endGameButton.setVisibility(View.GONE);
    			switchTableButton.setVisibility(View.VISIBLE);
    			buttonPanel.setVisibility(View.VISIBLE);

    			long score = field.getGameState().getScore();
    			if (score > this.highScore) {
    				this.updateHighScoreForCurrentLevel(score);
    			}
    		}
    	}
    }
    
    // store separate high scores for each field, using unique suffix in prefs key
    String highScorePrefsKeyForLevel(int theLevel) {
    	return HIGHSCORE_PREFS_KEY + "." + theLevel;
    }
    
    /** Returns the high score stored in SharedPreferences, or 0 if no score is stored. */
    long highScoreFromPreferences(int theLevel) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	long score = prefs.getLong(highScorePrefsKeyForLevel(theLevel), 0);
    	if (score==0 && theLevel==1) {
    		// check for no level suffix, to read pre-1.3 scores
    		score = prefs.getLong(HIGHSCORE_PREFS_KEY, 0);
    	}
    	return score;
    }
    
    long highScoreFromPreferencesForCurrentLevel() {
    	return highScoreFromPreferences(level);
    }
    
    /** Updates the highScore instance variable, the ScoreView display, and writes the score to SharedPreferences. */
    void updateHighScore(int theLevel, long score) {
    	this.highScore = score;
    	scoreView.setHighScore(score);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putLong(highScorePrefsKeyForLevel(theLevel), score);
    	editor.commit();
    }
    
    void updateHighScoreForCurrentLevel(long score) {
    	updateHighScore(level, score);
    }
    
    int getInitialLevel() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	int startLevel = prefs.getInt(INITIAL_LEVEL_PREFS_KEY, 1);
    	if (startLevel<1 || startLevel>FieldLayout.numberOfLevels()) startLevel = 1;
    	return startLevel;
    }
    
    void setInitialLevel(int level) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    	SharedPreferences.Editor editor = prefs.edit();
    	editor.putInt(INITIAL_LEVEL_PREFS_KEY, level);
    	editor.commit();
    }
    
    // button action methods (defined by android:onClick values in main.xml)
    public void doStartGame(View view) {
        if (field.getGameState().isPaused()) {
            unpauseGame();
            return;
        }
    	// avoids accidental starts due to touches just after game ends
    	if (endGameTime==null || (System.currentTimeMillis() < endGameTime + END_GAME_DELAY)) return;
    	if (!field.getGameState().isGameInProgress()) {
            buttonPanel.setVisibility(View.GONE);
            field.resetForLevel(this, level);
            field.startGame();
            VPSoundpool.playStart();
            endGameTime = null;
    	}
    }

    public void doEndGame(View view) {
        // Game might be paused, if ended from button.
        unpauseGame();
        field.endGame();
    }
    
    public void doPreferences(View view) {
    	gotoPreferences();
    }
    
    public void doAbout(View view) {
    	gotoAbout();
    }

    public void scoreViewClicked(View view) {
        if (field.getGameState().isGameInProgress()) {
            if (field.getGameState().isPaused()) {
                unpauseGame();
            }
            else {
                pauseGame();
                showPausedButtons();
            }
        }
        else {
            doStartGame(null);
        }
    }
    
    public void doSwitchTable(View view) {
    	level = (level==FieldLayout.numberOfLevels()) ? 1 : level+1;
    	synchronized(field) {
        	field.resetForLevel(this, level);
    	}
    	this.setInitialLevel(level);
        this.highScore = this.highScoreFromPreferencesForCurrentLevel();
        scoreView.setHighScore(highScore);
    }
}