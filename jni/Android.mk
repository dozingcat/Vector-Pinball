LOCAL_PATH := $(call my-dir)

#
# FMOD Ex Shared Library
# 
include $(CLEAR_VARS)

LOCAL_MODULE            := fmodex
LOCAL_SRC_FILES         := ../../fmodexapi/api/lib/android-3/$(TARGET_ARCH_ABI)/libfmodex.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../fmodexapi/api/inc

include $(PREBUILT_SHARED_LIBRARY)

#
# FMOD Event Shared Library
# 
include $(CLEAR_VARS)

LOCAL_MODULE            := fmodevent
LOCAL_SRC_FILES         := ../../fmodexapi/fmodeventapi/api/lib/$(TARGET_ARCH_ABI)/libfmodevent.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../fmodexapi/fmodeventapi/api/inc

include $(PREBUILT_SHARED_LIBRARY)

#
# gdx Shared Library
# 
include $(CLEAR_VARS)

LOCAL_MODULE            := gdx
LOCAL_SRC_FILES         := ../../gdxlibs/$(TARGET_ARCH_ABI)/libandroidgl20.so
LOCAL_SRC_FILES         := ../../gdxlibs/$(TARGET_ARCH_ABI)/libgdx.so
##LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../../fmodexapi/fmodeventapi/api/inc

include $(PREBUILT_SHARED_LIBRARY)



#
# VPS Library
#
include $(CLEAR_VARS)

LOCAL_MODULE           := main
LOCAL_SRC_FILES        := main.c
LOCAL_LDLIBS 		   := -llog
LOCAL_SHARED_LIBRARIES := fmodex fmodevent

include $(BUILD_SHARED_LIBRARY)


###test
