package com.dozingcatsoftware.bouncy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

public class VPSoundpool {

    private static SoundPool mSoundPool; 
    private static HashMap<Integer, Integer> mSoundPoolMap; 
    private static AudioManager  mAudioManager;
    private static Context mContext;
    private static Random mRandom = new Random();
    
    private static boolean soundEnabled = true;
    private static boolean musicEnabled = true;
	private static int cScore = 0;
	private static int prevDing, currentDing = 0;
	private static int andrModAmt = 10;
	private static MediaPlayer drumbass;
	private static boolean drumbassPlaying = false;
	private static MediaPlayer androidpad;
    //private final static float SEMITONE = 1.059463094f;
    
    static int ID_DING_START 	= 0;
    static int NUM_DINGS 		= 6;
    
    static int ID_LAUNCH 		= 100;
    static int ID_FLIPPER 		= 101;
    static int ID_MESSAGE 		= 102;
    static int ID_START 		= 103;
    
    static int ID_ROLLOVER 		= 200;
    
    static int ID_ANDROIDPAD 	= 300;
    static int ID_DRUMBASSLOOP 	= 301;
    

    public static void initSounds(Context theContext) 
    { 
         Log.v("VPSoundpool", "initSounds");
         mContext = theContext;
         mSoundPool = new SoundPool(32, AudioManager.STREAM_MUSIC, 0);
         mSoundPoolMap = new HashMap<Integer, Integer>(); 
         mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
    } 

    public static void loadSounds()
    {
        mSoundPoolMap.clear();
        AssetManager assets = mContext.getAssets();
        try {
            mSoundPoolMap.put(ID_DING_START+0, mSoundPool.load(assets.openFd("audio/bumper/dinga1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+1, mSoundPool.load(assets.openFd("audio/bumper/dingc1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+2, mSoundPool.load(assets.openFd("audio/bumper/dingc2.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+3, mSoundPool.load(assets.openFd("audio/bumper/dingd1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+4, mSoundPool.load(assets.openFd("audio/bumper/dinge1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+5, mSoundPool.load(assets.openFd("audio/bumper/dingg1.ogg"), 1));
            
            mSoundPoolMap.put(ID_LAUNCH, mSoundPool.load(assets.openFd("audio/misc/andBounce2.ogg"), 1));
            mSoundPoolMap.put(ID_FLIPPER, mSoundPool.load(assets.openFd("audio/misc/flipper1.ogg"), 1));
            mSoundPoolMap.put(ID_MESSAGE, mSoundPool.load(assets.openFd("audio/misc/message2.ogg"), 1));
            mSoundPoolMap.put(ID_START, mSoundPool.load(assets.openFd("audio/misc/startup1.ogg"), 1));
            mSoundPoolMap.put(ID_ROLLOVER, mSoundPool.load(assets.openFd("audio/misc/rolloverE.ogg"), 1));
            
        	drumbass = MediaPlayer.create(mContext, R.raw.drumbassloop);
        	androidpad = MediaPlayer.create(mContext, R.raw.androidpad);

         }
        catch(IOException ex) {
        	Log.e("VPSoundpool", "Error loading sounds", ex);
        }
        resetMusicState();
    }
    
    public static void setSoundEnabled(boolean enabled) {
    	soundEnabled = enabled;
    }
    
    public static void setMusicEnabled(boolean enabled) {
    	musicEnabled = enabled;
    	if (!musicEnabled) {
    		resetMusicState();
    	}
    }
    
    public static void resetMusicState() {
    	pauseMusic();
    	drumbassPlaying = false;
    	cScore = 0;
    	andrModAmt = 10;
    	if (drumbass!=null) drumbass.seekTo(0);
    	if (androidpad!=null) androidpad.seekTo(0);
    }
    
    static void playSound(int soundKey, float volume, float pitch) {
    	if (soundEnabled && mSoundPoolMap!=null) {
    		Integer soundID = mSoundPoolMap.get(soundKey);
    		if (soundID!=null) {
        		mSoundPool.play(soundID, volume, volume, 1, 0, pitch);
    		}
    	}
    }

    public static void playScore() {
    	//prevent repeated dings
    	while (currentDing == prevDing) {
        	currentDing = mRandom.nextInt(NUM_DINGS);    		
    	}
    	playSound(ID_DING_START + currentDing, 0.5f, 1);
    	prevDing = currentDing;

    	//start playing the drumbassloop after 20 scoring hits
    	cScore++;
    	if (musicEnabled && cScore%20 == 0 && drumbass!=null && !drumbass.isPlaying()){
    		drumbass.setVolume(1.0f, 1.0f);
    	    drumbass.start();
    	    drumbassPlaying = true;
    	}
    	//play the androidpad after 10 scoring hits the first time
    	//then increase the mod amount each time the pad is played,
    	//so that it will be heard less frequently over time
    	if (musicEnabled && androidpad!=null && cScore%andrModAmt == 0){
    		androidpad.setVolume(0.5f, 0.5f);
    	    androidpad.start();
    		andrModAmt += 42;
    	}
    }

    public static void playRollover() {
    	//play up to three events, each randomly pitched to a different note in the pentatonic scale
    	//the rollover ding is E, so in semitones, the other pitches are -4 (C), -2 (D), +3 (G), +5 (A), +8 (C)
    	//for Soundpool, that translate to:  0.7937008 (C), 0.8908991 (D), 1.1892079 (G), 1.3348408 (A), 1.5874025 (C)
    	float pitch[] = {0.7937008f, 0.8908991f, 1f, 1.1892079f, 1.3348408f, 1.5874025f};
    	int pitchDx[] = {0, 0, 0};

        for (int i = 0; i < 3; i++) {
    		switch (i){
    			case 0:
    				pitchDx[i] = mRandom.nextInt(6);
    		    	playSound(ID_ROLLOVER, .3f, pitch[pitchDx[i]]);
    				break;
    			case 1:
    				pitchDx[i] = mRandom.nextInt(6);
    				if (pitchDx[i] != pitchDx[i-1]) {
        		    	playSound(ID_ROLLOVER, .3f, pitch[pitchDx[i]]);
    				}
    				break;
    			case 2:
    				pitchDx[i] = mRandom.nextInt(6);
    				if (pitchDx[i] != pitchDx[i-1] &&
    					pitchDx[i] != pitchDx[i-2] ) {
        		    	playSound(ID_ROLLOVER, .3f, pitch[pitchDx[i]]);
    				}
    				break;
    			default:
    				Log.e("VPSoundpool", "rollover bad mojo");
    				break;
    		}
        }
    }
    
    public static void playBall() {
    	playSound(ID_LAUNCH, 1, 1);
    }
    
    public static void playFlipper() {
    	playSound(ID_FLIPPER, 1, 1);
    }
    
    public static void playStart() {
    	resetMusicState();
    	playSound(ID_START, 0.5f, 1);
    }
    
    public static void playMessage() {
    	playSound(ID_MESSAGE, 0.66f, 1);
    }
    
    public static void pauseMusic()
    {
    	if (drumbass!=null && drumbass.isPlaying()) {
            drumbass.pause();
    	}
    	if (androidpad!=null && androidpad.isPlaying()) {
    		androidpad.pause();
    	}
    }
    public static void resumeMusic()
    {
    	if (drumbass!=null && drumbassPlaying) {
    		drumbass.start();
    	}
        //androidpad.start();
    }
    public static void stopMusic()
    {
        if (drumbass!=null) drumbass.stop();
        drumbassPlaying = false;
        if (androidpad!=null) androidpad.stop();
    }
    
    public static void cleanup()
    {
        mSoundPool.release();
        mSoundPool = null;
        mSoundPoolMap.clear();
        mAudioManager.unloadSoundEffects();
        drumbass.release();
        drumbass = null;
        androidpad.release();
        androidpad = null;
    }
}
