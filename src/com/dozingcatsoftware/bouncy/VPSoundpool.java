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
    
    static int ID_DING_START = 0;
    static int NUM_DINGS = 6;
    
    static int ID_LAUNCH = 100;
    static int ID_FLIPPER = 101;
    static int ID_MESSAGE = 102;
    static int ID_START = 103;
    
    static int ID_ROLLOVER = 200;

    public static void initSounds(Context theContext) 
    { 
         Log.v("VPSoundpool", "initSounds");
         mContext = theContext;
         mSoundPool = new SoundPool(4, AudioManager.STREAM_SYSTEM, 0);
         mSoundPoolMap = new HashMap<Integer, Integer>(); 
         mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
    } 

    public static void loadSounds()
    {
        mSoundPoolMap.clear();
        
        AssetManager assets = mContext.getAssets();
        try {
            mSoundPoolMap.put(ID_DING_START+0, mSoundPool.load(assets.openFd("audio/dings/xdinga1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+1, mSoundPool.load(assets.openFd("audio/dings/xdingc1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+2, mSoundPool.load(assets.openFd("audio/dings/xdingc2.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+3, mSoundPool.load(assets.openFd("audio/dings/xdingd1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+4, mSoundPool.load(assets.openFd("audio/dings/xdinge1.ogg"), 1));
            mSoundPoolMap.put(ID_DING_START+5, mSoundPool.load(assets.openFd("audio/dings/xdingg1.ogg"), 1));
            
            mSoundPoolMap.put(ID_LAUNCH, mSoundPool.load(assets.openFd("audio/misc/andBounce2.ogg"), 1));
            mSoundPoolMap.put(ID_FLIPPER, mSoundPool.load(assets.openFd("audio/misc/flipper1.ogg"), 1));
            mSoundPoolMap.put(ID_MESSAGE, mSoundPool.load(assets.openFd("audio/misc/message2.ogg"), 1));
            mSoundPoolMap.put(ID_START, mSoundPool.load(assets.openFd("audio/misc/startup1.ogg"), 1));
            mSoundPoolMap.put(ID_ROLLOVER, mSoundPool.load(assets.openFd("audio/misc/rolloverE.ogg"), 1));            
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
    	playSound(ID_DING_START + mRandom.nextInt(NUM_DINGS), 1);
    }

    public static void playRollover() {
    	// play multiple notes with random pitch?
    	playSound(ID_ROLLOVER, 1);
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
