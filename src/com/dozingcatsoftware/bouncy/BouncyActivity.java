package com.dozingcatsoftware.bouncy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
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

	FMODaudio mFMODaudio = new FMODaudio();

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
        mFMODaudio.updateOnCreate();
    }
    
    @Override
    public void onStart()
    { 	
    	super.onStart();
/*
 * 		During initialization, FMOD requires a pathway to the folder where the event and soundbank files are stored.
 * 		The files in the Assets folder must be copied out of the (compressed) Android package (.apk)
 * 		to either internal storage, or the external SD card.
 * 		Then a string containing the pathway is passed down to the native C code.
 */
    	
    	// to sdcard:
//        AssetManager mAssetManager = getAssets();
//        String[] files = null;
//        try {
//            files = mAssetManager.list("fmod");
//        } catch (IOException e) {
//            Log.e("BouncyActivity", e.getMessage());
//        }
//        
//        Log.e("BouncyActivity", "files= " + files[0]);
//
//        for(int i=0; i<files.length; i++) {
//            InputStream in = null;
//            try {
//              in = mAssetManager.open("fmod/" + files[i], 0);
//              
//              File sdCard = Environment.getExternalStorageDirectory();
//              File dir = new File (sdCard.getAbsolutePath() + "/fmod");
//              dir.mkdirs();
//              File file = new File(dir, files[i]);
//              FileOutputStream fout = new FileOutputStream(file);
//
//              copyFile(in, fout);
//              in.close();
//              in = null;
//              fout.flush();
//              fout.close();
//              fout = null;
//            } catch(Exception e) {
//                Log.e("BouncyActivity Exception", e.getMessage());
//            }       
//        }
    	
    	//to internal storage:
      AssetManager mAssetManager = getAssets();
      String[] FMODfiles = null;
      try {
          FMODfiles = mAssetManager.list("fmod");
      } catch (IOException e) {
          Log.e("BouncyActivity", e.getMessage());
      }
      
      File mediaDir = getDir("fmod", 0);
      for(int i=0; i<FMODfiles.length; i++) {
          File fileOut = new File(mediaDir, FMODfiles[i]);
          if (!fileOut.exists()) {
	          try {
	        	  InputStream in = mAssetManager.open("fmod/" + FMODfiles[i], 0);
		          FileOutputStream out = new FileOutputStream(fileOut);
		          copyFile(in, out);
		          in.close();
		          in = null;
		          out.flush();
		          out.close();
		          out = null;
	          } catch(Exception e) {
	              Log.e("BouncyActivity Exception", e.getMessage());
	          }
          }
      }
      String mediaPath = mediaDir.getPath();
      mFMODaudio.initAudio(mediaPath);
    }
    
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
          out.write(buffer, 0, read);
        }
    }

    @Override
    public void onStop()
    { 	
    	mFMODaudio.stopAudio();
     	super.onStop();
     	
     	// workaround for FMOD MusicSystem memory release bug: kill the process to allow a clean restart
     	// FMOD bug fix scheduled for release version 4.35.06 
     	System.exit(0);
    }
    
    void gotoPreferences() {
		Intent settingsActivity = new Intent(getBaseContext(), BouncyPreferences.class);
		startActivityForResult(settingsActivity, ACTIVITY_PREFERENCES);
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
    		Intent aboutIntent = new Intent(getBaseContext(), AboutActivity.class);
    		this.startActivity(aboutIntent);
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
    	field.startGame();
    }
    
    public void doPreferences(View view) {
    	gotoPreferences();
    }
    
    public void doSwitchTable(View view) {
    	level = (level==FieldLayout.numberOfLevels()) ? 1 : level+1;
    	field.resetForLevel(this, level);
    }
}