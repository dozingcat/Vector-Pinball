/*===============================================================================================
 Java access to the native FMOD libraries.
 peter "pdx" drescher
 www.twittering.com

===============================================================================================*/
package com.dozingcatsoftware.bouncy;

import org.fmod.FMODAudioDevice;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class FMODaudio {

	private FMODAudioDevice mFMODAudioDevice = new FMODAudioDevice();
	int cScore = 0;
	int andrModAmt = 10;
	  
	private Handler mUpdateHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{	
			// call system update every 50ms
			cUpdate();
			removeMessages(0);
		    sendMessageDelayed(obtainMessage(0), 50);
		}
	};	
	
    public void initAudio(String mediaPath) //onStart
    {
    	mFMODAudioDevice.start();
    	cBegin(mediaPath);
    	mUpdateHandler.sendMessageDelayed(mUpdateHandler.obtainMessage(0), 0);
    	cStart();
    }

    public void updateOnCreate() //onCreate
    {
    	mUpdateHandler.sendMessageDelayed(mUpdateHandler.obtainMessage(0), 0);
    }

    public void stopAudio() //onStop
    {
    	mUpdateHandler.removeMessages(0);
		Log.d("FMODaudio", "stopAudio");
    	cEnd();
    	mFMODAudioDevice.stop();
    }
    
    public void playStart()
    {
    	cStart();
    }	

    public void playBall()
    {
    	cPlayBall();
    }	

    public void playRollover()
    {
    	cPlayRollover();
    }	

    public void playFlipper()
    {
    	cPlayFlipper();
    }	

    public void playMessage(String text)
    {
       if ( (text.equals("Ball 2")) ||
        	(text.equals("Ball 3")) ||	
        	(text.equals("Game Over")) ) {
    		//Log.d("FMODaudio", "playMessage= " + text);
    	} else {
    		cPlayMessage();
    	}
    }	

    public void playScore()
    {
    	cPlayScore(); //ding!
    	
    	//change the track every (mod) number of hits
    	cScore++;
    	if (cScore%10 == 0){
    		cDoBassTrack();
    	}
    	if (cScore%12 == 0){
    		cDoDrumTrack();
    	}
    	//each time the android track is played, increase the mod amount,
    	//so that the track will be heard less frequently over time
    	if (cScore%andrModAmt == 0){
    		andrModAmt += 42;
    		cDoAndroidTrack();
    	}
    }	

    static 
    {
    	System.loadLibrary("fmodex");
    	System.loadLibrary("fmodevent");
        System.loadLibrary("main");
    }
   
	public native void cBegin(String mediaPath);
	public native void cUpdate();
	public native void cEnd();
	public native void cStart();
	public native void cPlayScore();
	public native void cPlayBall();
	public native void cPlayRollover();
	public native void cPlayFlipper();
	public native void cPlayMessage();
	public native void cDoBassTrack();
	public native void cDoDrumTrack();
	public native void cDoAndroidTrack();  
}