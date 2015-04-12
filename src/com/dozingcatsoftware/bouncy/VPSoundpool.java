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
    private static MediaPlayer androidpad;
    //private final static float SEMITONE = 1.059463094f;

    private static volatile boolean soundsLoaded = false;

    static int ID_DING_START 	= 0;
    static int NUM_DINGS 		= 6;

    static int ID_LAUNCH 		= 100;
    static int ID_FLIPPER 		= 101;
    static int ID_MESSAGE 		= 102;
    static int ID_START 		= 103;

    static int ID_ROLLOVER 		= 200;

    static int ID_ANDROIDPAD 	= 300;
    static int ID_DRUMBASSLOOP 	= 301;

    private static final String LOG_TAG = "VPSoundPool";

    public static void initSounds(Context theContext) {
        Log.v(LOG_TAG, "initSounds");
        mContext = theContext;
        mSoundPool = new SoundPool(32, AudioManager.STREAM_MUSIC, 0);
        mSoundPoolMap = new HashMap<Integer, Integer>();
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    // On some devices running Lollipop (seen on Nexus 5 and emulator), SoundPool slows down
    // significantly after loading ~6 sounds, taking several seconds for each additional call to
    // load(). To avoid blocking the main thread (and delaying the app launch), this method is
    // called in an AsyncTask from the main activity.
    // See https://code.google.com/p/android-developer-preview/issues/detail?id=1812
    public static void loadSounds() {
        Log.v(LOG_TAG, "loadSounds start");
        soundsLoaded = false;
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

            soundsLoaded = true;
            resetMusicState();
            Log.v(LOG_TAG, "loadSounds finished");
        }
        catch(IOException ex) {
            Log.e(LOG_TAG, "Error loading sounds", ex);
        }
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
        if (!soundsLoaded) return;

        pauseMusic();
        cScore = 0;
        andrModAmt = 10;
        if (drumbass!=null) drumbass.seekTo(0);
        if (androidpad!=null) androidpad.seekTo(0);
    }

    static void playSound(int soundKey, float volume, float pitch) {
        if (!soundsLoaded) return;

        if (soundEnabled && mSoundPoolMap!=null) {
            Integer soundID = mSoundPoolMap.get(soundKey);
            if (soundID!=null) {
                mSoundPool.play(soundID, volume, volume, 1, 0, pitch);
            }
        }
    }

    public static void playScore() {
        if (!soundsLoaded) return;

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

    // Play up to three events, each randomly pitched to a different note in the pentatonic scale.
    // The rollover ding is E, so in semitones, the other pitches are
    // -4 (C), -2 (D), +3 (G), +5 (A), +8 (C). For Soundpool, that translates to:
    // 0.7937008 (C), 0.8908991 (D), 1.1892079 (G), 1.3348408 (A), 1.5874025 (C).
    private static final float[] PITCHES = {
        0.7937008f, 0.8908991f, 1f, 1.1892079f, 1.3348408f, 1.5874025f
    };
    private static final float ROLLOVER_DURATION = 0.3f;

    public static void playRollover() {
        if (!soundsLoaded) return;

        int p1 = mRandom.nextInt(6);
        int p2 = mRandom.nextInt(6);
        int p3 = mRandom.nextInt(6);

        // Only play distinct notes.
        playSound(ID_ROLLOVER, ROLLOVER_DURATION, PITCHES[p1]);
        if (p2 != p1) {
            playSound(ID_ROLLOVER, ROLLOVER_DURATION, PITCHES[p2]);
        }
        if (p3 != p1 && p3 != p2) {
            playSound(ID_ROLLOVER, ROLLOVER_DURATION, PITCHES[p3]);
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

    public static void pauseMusic() {
        if (!soundsLoaded) return;

        if (drumbass!=null && drumbass.isPlaying()) {
            drumbass.pause();
        }
        if (androidpad!=null && androidpad.isPlaying()) {
            androidpad.pause();
        }
    }

    public static void cleanup() {
        Log.v(LOG_TAG, "cleanup start");
        mSoundPool.release();
        mSoundPool = null;
        mSoundPoolMap.clear();
        mAudioManager.unloadSoundEffects();
        drumbass.release();
        drumbass = null;
        androidpad.release();
        androidpad = null;
        Log.v(LOG_TAG, "cleanup finished");
    }

    public static class Player implements AudioPlayer {
        @Override public void playStart() {
            VPSoundpool.playStart();
        }

        @Override public void playBall() {
            VPSoundpool.playBall();
        }

        @Override public void playFlipper() {
            VPSoundpool.playFlipper();
        }

        @Override public void playScore() {
            VPSoundpool.playScore();
        }

        @Override public void playMessage() {
            VPSoundpool.playMessage();
        }

        @Override public void playRollover() {
            VPSoundpool.playRollover();
        }
    }
}
