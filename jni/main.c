/*===============================================================================================
 Native C code for accessing FMOD libraries.
 peter "pdx" drescher
 www.twittering.com

===============================================================================================*/

#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include "../../fmodexapi/fmodeventapi/api/inc/fmod_event.h"
#include "../../fmodexapi/api/inc/fmod_errors.h"
#include <VPS2.h>

FMOD_SYSTEM  *gSystem  = 0;
FMOD_EVENTSYSTEM *gEventSystem  = 0;
FMOD_MUSICSYSTEM *gMusicSystem  = 0;
FMOD_EVENT *gEvent = 0;
FMOD_EVENTGROUP *gEventGroup = 0;
FMOD_EVENTPARAMETER *gBassTrackLevel = 0;
FMOD_MUSICPROMPT *gAndroid = 0;
FMOD_MUSICPROMPT *gBass = 0;
FMOD_MUSICPROMPT *gDrLoop1 = 0;
FMOD_MUSICPROMPT *gDrLoop2 = 0;
FMOD_MUSICPROMPT *gDrLoop3 = 0;

unsigned int bufferLength = 256;
int numBuffers = 8;
float bassSeq = 0;
int ctr, drumTrax = 0;
int segmentEnd, androidTrackPlayed = 0;

#define CHECK_RESULT(x) \
{ \
	FMOD_RESULT _result = x; \
	if (_result != FMOD_OK) \
	{ \
		__android_log_print(ANDROID_LOG_ERROR, "fmod", "FMOD error! (%d) %s\n%s:%d", _result, FMOD_ErrorString(_result), __FILE__, __LINE__); \
		exit(-1); \
	} \
}

FMOD_RESULT F_CALLBACK segmentCallback(FMOD_MUSIC_CALLBACKTYPE type, void *param1, void *param2, void *userdata)
{
	FMOD_RESULT result = FMOD_OK;
	switch (type)
	{
		case FMOD_MUSIC_CALLBACKTYPE_SEGMENT_END:
		{
			segmentEnd = (uintptr_t)param1;
			if (segmentEnd == MUSICSEGMENT_VPS2_ANDROID_THEME_ANDROID1){
				// when the android1 segment finishes playing for the first time, start the bass track
				if (!androidTrackPlayed) {
					androidTrackPlayed = 1;
					result = FMOD_MusicSystem_SetParameterValue(gMusicSystem, MUSICPARAM_VPS2_BASSSEQUENCE, 0);
					CHECK_RESULT(result);
					result = FMOD_MusicPrompt_Begin(gBass);
					CHECK_RESULT(result);
				}
			}
		}
	}
	return FMOD_OK;
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cBegin(JNIEnv *env, jobject thiz, jstring mediaPath)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL cacheevents = 0;
	srand (time(NULL));
    const char *_mediaPath = (*env)->GetStringUTFChars (env, mediaPath, 0);
    strcat (_mediaPath, "/");

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "create event system");
	result = FMOD_EventSystem_Create(&gEventSystem);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "getSystemObject");
	result = FMOD_EventSystem_GetSystemObject(gEventSystem, &gSystem);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "set DSPBufferSize");
	result = FMOD_System_SetDSPBufferSize(gSystem, bufferLength, numBuffers);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "init event system");
	result = FMOD_EventSystem_Init(gEventSystem, 64, FMOD_INIT_NORMAL, 0, FMOD_EVENT_INIT_NORMAL);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "set media path= %s", _mediaPath);
	//result = FMOD_EventSystem_SetMediaPath(gEventSystem, "sdcard/fmod/");
	result = FMOD_EventSystem_SetMediaPath(gEventSystem, _mediaPath);
	CHECK_RESULT(result);
    (*env)->ReleaseStringUTFChars (env, mediaPath, _mediaPath);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "load eventsystem");
	result = FMOD_EventSystem_Load(gEventSystem, "VPS2.fev", 0, 0);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "get initial events");
	//sometimes there is a noticeable pause when this event plays for the first time
	//getting it during initialization seems to sidestep the issue
	result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/dings", FMOD_EVENT_DEFAULT, &gEvent);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "get musicsystem");
	result = FMOD_EventSystem_GetMusicSystem(gEventSystem, &gMusicSystem);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "load samples");
	result = FMOD_MusicSystem_LoadSoundData(gMusicSystem, FMOD_EVENT_RESOURCE_SAMPLES, FMOD_EVENT_DEFAULT);
	CHECK_RESULT(result);

	__android_log_print(ANDROID_LOG_ERROR, "fmod", "prepare cues");
	result = FMOD_MusicSystem_PrepareCue(gMusicSystem, MUSICCUE_VPS2_ANDROID, &gAndroid);
	CHECK_RESULT(result);
	result = FMOD_MusicSystem_PrepareCue(gMusicSystem, MUSICCUE_VPS2_BASS,    &gBass);
	CHECK_RESULT(result);
	result = FMOD_MusicSystem_PrepareCue(gMusicSystem, MUSICCUE_VPS2_DRLOOP1, &gDrLoop1);
	CHECK_RESULT(result);
	result = FMOD_MusicSystem_PrepareCue(gMusicSystem, MUSICCUE_VPS2_DRLOOP2, &gDrLoop2);
	CHECK_RESULT(result);
	result = FMOD_MusicSystem_PrepareCue(gMusicSystem, MUSICCUE_VPS2_DRLOOP3, &gDrLoop3);
	CHECK_RESULT(result);

	result = FMOD_MusicSystem_SetCallback(gMusicSystem, segmentCallback, 0);
	CHECK_RESULT(result);
	androidTrackPlayed = 0;
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cUpdate(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT	result = FMOD_OK;
	//called every 50ms by FMODaudio.java
	result = FMOD_EventSystem_Update(gEventSystem);
	CHECK_RESULT(result);
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cEnd(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL waituntilready = 0;

	result = FMOD_EventSystem_Release(gEventSystem);
	CHECK_RESULT(result);
	//exit(0); // workaround for MusicSystem memory release bug, to be fixed in version FMOD API 4.35.06
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cStart(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;

	result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/startup", FMOD_EVENT_DEFAULT, &gEvent);
	CHECK_RESULT(result);
	result = FMOD_Event_Start(gEvent);
	CHECK_RESULT(result);
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cPlayScore(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/dings", FMOD_EVENT_DEFAULT, &gEvent);
	CHECK_RESULT(result);
	result = FMOD_Event_Start(gEvent);
	CHECK_RESULT(result);
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cPlayRollover(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_EVENT_PITCHUNITS units = FMOD_EVENT_PITCHUNITS_SEMITONES;

	//play up to three events, each randomly pitched to a different note in the pentatonic scale
	//the rollover ding is E, so in semitones, the other pitches are -4 (C), -2 (D), +3 (G), +5 (A), +8 (C)
	float pitch[] = {-4, -2, 0, 3, 5, 8};
	int pitchDx[] = {0, 0, 0};
	int i;
    for (i = 0; i < 3; i++) {
		switch (i){
			case 0:
				result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/rollover", FMOD_EVENT_DEFAULT, &gEvent);
				CHECK_RESULT(result);
				pitchDx[i] = (rand() % 6);
				result = FMOD_Event_SetPitch(gEvent, pitch[pitchDx[i]], units);
				CHECK_RESULT(result);
				result = FMOD_Event_Start(gEvent);
				CHECK_RESULT(result);
				break;
			case 1:
				pitchDx[i] = (rand() % 6);
				if (pitchDx[i] != pitchDx[i-1]) {
					result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/rollover", FMOD_EVENT_DEFAULT, &gEvent);
					CHECK_RESULT(result);
					result = FMOD_Event_SetPitch(gEvent, pitch[pitchDx[i]], units);
					CHECK_RESULT(result);
					result = FMOD_Event_Start(gEvent);
					CHECK_RESULT(result);
				}
				break;
			case 2:
				pitchDx[i] = (rand() % 6);
				if (pitchDx[i] != pitchDx[i-1] &&
					pitchDx[i] != pitchDx[i-2] ) {
					result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/rollover", FMOD_EVENT_DEFAULT, &gEvent);
					CHECK_RESULT(result);
					result = FMOD_Event_SetPitch(gEvent, pitch[pitchDx[i]], units);
					CHECK_RESULT(result);
					result = FMOD_Event_Start(gEvent);
					CHECK_RESULT(result);
				}
				break;
			default:
				__android_log_print(ANDROID_LOG_ERROR, "fmod", "rollover bad mojo");
				break;
		}
    }
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cPlayBall(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;

	result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/bouncyBall", FMOD_EVENT_DEFAULT, &gEvent);
	CHECK_RESULT(result);
	result = FMOD_Event_Start(gEvent);
	CHECK_RESULT(result);
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cPlayFlipper(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;

	result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/flipper", FMOD_EVENT_DEFAULT, &gEvent);
	CHECK_RESULT(result);
	result = FMOD_Event_Start(gEvent);
	CHECK_RESULT(result);
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cPlayMessage(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;

	result = FMOD_EventSystem_GetEvent(gEventSystem, "VPS2/VPS2/message", FMOD_EVENT_DEFAULT, &gEvent);
	CHECK_RESULT(result);
	result = FMOD_Event_Start(gEvent);
	CHECK_RESULT(result);
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cDoBassTrack(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL active;
	if (androidTrackPlayed) {
		FMOD_MusicPrompt_IsActive (gBass, &active);
		CHECK_RESULT(result);
		if (!active){
			result = FMOD_MusicPrompt_Begin(gBass);
			CHECK_RESULT(result);
		}
		result = FMOD_MusicSystem_SetParameterValue(gMusicSystem, MUSICPARAM_VPS2_BASSSEQUENCE, bassSeq);
		CHECK_RESULT(result);
		//cycle through the bass sequences
		bassSeq++;
		if (bassSeq > 2)
			bassSeq = 0;
	}
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cDoDrumTrack(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL active;
	if (androidTrackPlayed) {
		FMOD_MusicPrompt_IsActive (gDrLoop1, &active);
		CHECK_RESULT(result);
		if (active) {
			result = FMOD_MusicPrompt_End(gDrLoop1);
			CHECK_RESULT(result);
		}
		FMOD_MusicPrompt_IsActive (gDrLoop2, &active);
		CHECK_RESULT(result);
		if (active) {
			result = FMOD_MusicPrompt_End(gDrLoop2);
			CHECK_RESULT(result);
		}
		FMOD_MusicPrompt_IsActive (gDrLoop3, &active);
		CHECK_RESULT(result);
		if (active) {
			result = FMOD_MusicPrompt_End(gDrLoop3);
			CHECK_RESULT(result);
		}

		//play the first three tracks in sequence, then in random combinations
		ctr++;
		if (ctr > 3)
			drumTrax = (rand() % 6)+1;
		else
			drumTrax = ctr;

		switch (drumTrax){
			case 1:
				result = FMOD_MusicPrompt_Begin(gDrLoop1);
				CHECK_RESULT(result);
				break;
			case 2:
				result = FMOD_MusicPrompt_Begin(gDrLoop2);
				CHECK_RESULT(result);
				break;
			case 3:
				result = FMOD_MusicPrompt_Begin(gDrLoop3);
				CHECK_RESULT(result);
				break;
			case 4:
				result = FMOD_MusicPrompt_Begin(gDrLoop1);
				CHECK_RESULT(result);
				result = FMOD_MusicPrompt_Begin(gDrLoop2);
				CHECK_RESULT(result);
				break;
			case 5:
				result = FMOD_MusicPrompt_Begin(gDrLoop2);
				CHECK_RESULT(result);
				result = FMOD_MusicPrompt_Begin(gDrLoop3);
				CHECK_RESULT(result);
				break;
			case 6:
				result = FMOD_MusicPrompt_Begin(gDrLoop1);
				CHECK_RESULT(result);
				result = FMOD_MusicPrompt_Begin(gDrLoop3);
				CHECK_RESULT(result);
				break;
			default:
				__android_log_print(ANDROID_LOG_ERROR, "fmod", "drum track bad mojo");
				break;
		}
	}
}

void Java_com_dozingcatsoftware_bouncy_FMODaudio_cDoAndroidTrack(JNIEnv *env, jobject thiz)
{
	FMOD_RESULT result = FMOD_OK;
	FMOD_BOOL active;

	FMOD_MusicPrompt_IsActive (gAndroid, &active);
	if (active){
		result = FMOD_MusicPrompt_End(gAndroid);
		CHECK_RESULT(result);
	}
	result = FMOD_MusicPrompt_Begin(gAndroid);
	CHECK_RESULT(result);
}
