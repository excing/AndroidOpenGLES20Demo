LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := freetype

LOCAL_C_INCLUDES := ${LOCAL_PATH}/../freetype-2.10.2/include

LOCAL_SRC_FILES := \
 jni00.c \
 jni01.c \
 jni02.c

LOCAL_STATIC_LIBRARIES := freetype2-static

LOCAL_LDLIBS := -llog \
 -lGLESv2

include $(BUILD_SHARED_LIBRARY)
