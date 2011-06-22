package com.dozingcatsoftware.bouncy;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

public class VPSoundpool {

    private static SoundPool mSoundPool; 
    private static HashMap<Integer, Integer> mSoundPoolMap; 
    private static AudioManager  mAudioManager;
    private static Context mContext;
    private static Random mRandom = new Random();
    
    private static boolean soundEnabled = true;
    
    static final int ID_BUMPER_START = 0;
    static final int NUM_BUMPER_SOUNDS = 6;
    
    static final int ID_LAUNCH = 100;
    static final int ID_FLIPPER = 101;
    static final int ID_MESSAGE = 102;
    static final int ID_START = 103;
    
    static final int ID_ROLLOVER_START = 200;
    static final int NUM_ROLLOVER_SOUNDS = 6;

    public static void initSounds(Context theContext) 
    { 
         Log.v("VPSoundpool", "initSounds");
         mContext = theContext;
         mSoundPool = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
         mSoundPoolMap = new HashMap<Integer, Integer>(); 
         mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
    } 

    public static void loadSounds()
    {
        mSoundPoolMap.clear();
        
        AssetManager assets = mContext.getAssets();
        try {
            mSoundPoolMap.put(ID_BUMPER_START+0, mSoundPool.load(assets.openFd("audio/bumper/xdinga1.ogg"), 1));
            mSoundPoolMap.put(ID_BUMPER_START+1, mSoundPool.load(assets.openFd("audio/bumper/xdingc1.ogg"), 1));
            mSoundPoolMap.put(ID_BUMPER_START+2, mSoundPool.load(assets.openFd("audio/bumper/xdingc2.ogg"), 1));
            mSoundPoolMap.put(ID_BUMPER_START+3, mSoundPool.load(assets.openFd("audio/bumper/xdingd1.ogg"), 1));
            mSoundPoolMap.put(ID_BUMPER_START+4, mSoundPool.load(assets.openFd("audio/bumper/xdinge1.ogg"), 1));
            mSoundPoolMap.put(ID_BUMPER_START+5, mSoundPool.load(assets.openFd("audio/bumper/xdingg1.ogg"), 1));
            
            mSoundPoolMap.put(ID_ROLLOVER_START+0, mSoundPool.load(assets.openFd("audio/rollover/rolloverC1.ogg"), 1));            
            mSoundPoolMap.put(ID_ROLLOVER_START+1, mSoundPool.load(assets.openFd("audio/rollover/rolloverD1.ogg"), 1));            
            mSoundPoolMap.put(ID_ROLLOVER_START+2, mSoundPool.load(assets.openFd("audio/rollover/rolloverE1.ogg"), 1));            
            mSoundPoolMap.put(ID_ROLLOVER_START+3, mSoundPool.load(assets.openFd("audio/rollover/rolloverG1.ogg"), 1));            
            mSoundPoolMap.put(ID_ROLLOVER_START+4, mSoundPool.load(assets.openFd("audio/rollover/rolloverA1.ogg"), 1));            
            mSoundPoolMap.put(ID_ROLLOVER_START+5, mSoundPool.load(assets.openFd("audio/rollover/rolloverC2.ogg"), 1));            

            mSoundPoolMap.put(ID_LAUNCH, mSoundPool.load(assets.openFd("audio/misc/andBounce2.ogg"), 1));
            mSoundPoolMap.put(ID_FLIPPER, mSoundPool.load(assets.openFd("audio/misc/flipper1.ogg"), 1));
            mSoundPoolMap.put(ID_MESSAGE, mSoundPool.load(assets.openFd("audio/misc/message2.ogg"), 1));
            mSoundPoolMap.put(ID_START, mSoundPool.load(assets.openFd("audio/misc/startup1.ogg"), 1));            
        }
        catch(IOException ex) {
        	Log.e("VPSoundpool", "Error loading sounds", ex);
        }
    }
    
    public static void setSoundEnabled(boolean enabled) {
    	soundEnabled = enabled;
    }
    
    static void playSound(int soundKey, float pitch) {
    	if (soundEnabled && mSoundPoolMap!=null) {
    		Integer soundID = mSoundPoolMap.get(soundKey);
    		if (soundID!=null) {
        		mSoundPool.play(soundID, 0.5f, 0.5f, 1, 0, pitch);
    		}
    	}
    }

    public static void playScore() {
    	playSound(ID_BUMPER_START + mRandom.nextInt(NUM_BUMPER_SOUNDS), 1);
    }

    // for rollovers, play chord of 1, 2, or 3 rollover notes
    static int[] rolloverNotes = new int[3];
    
    public static void playRollover() {
    	for(int i=0; i<3; i++) {
    		rolloverNotes[i] = mRandom.nextInt(NUM_ROLLOVER_SOUNDS);
    	}
    	
    	playSound(ID_ROLLOVER_START + rolloverNotes[0], 1);
    	if (rolloverNotes[1]!=rolloverNotes[0]) {
    		playSound(ID_ROLLOVER_START + rolloverNotes[1], 1);
    	}
    	if (rolloverNotes[2]!=rolloverNotes[0] && rolloverNotes[2]!=rolloverNotes[1]) {
    		playSound(ID_ROLLOVER_START + rolloverNotes[2], 1);
    	}
    	
    }
    
    public static void playBall() {
    	playSound(ID_LAUNCH, 1);
    }
    
    public static void playFlipper() {
    	playSound(ID_FLIPPER, 1);
    }
    
    public static void playStart() {
    	playSound(ID_START, 1);
    }
    
    public static void playMessage() {
    	playSound(ID_MESSAGE, 1);
    }
    
    public static void cleanup()
    {
        mSoundPool.release();
        mSoundPool = null;
        mSoundPoolMap.clear();
        mAudioManager.unloadSoundEffects();
    }
}
