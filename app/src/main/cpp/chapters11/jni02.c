/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
#include <string.h>
#include <jni.h>
#include <ft2build.h>
#include FT_FREETYPE_H

// 需要用 android log 日志打印时，需要在 Android.mk 文件里添加一句：
// LOCAL_LDLIBS := -llog
#include <android/log.h>

#define LOG_TAG "chpaters11"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT jint JNICALL
Java_me_excq_androidopengles20demo_chapters11_MainActivity_initTreeType( JNIEnv* env, jobject thiz, jstring fontPath )
{
    FT_Library ft;
    FT_Error error;

    error = FT_Init_FreeType(&ft);

    if (error) return error;

    const char* fontPathNativeString = (*env)->GetStringUTFChars(env, fontPath, 0);
    LOGI("font path is %s", fontPathNativeString);

    FT_Face face;
    error = FT_New_Face(ft, fontPathNativeString, 0, &face);

    if (error) return error;

    FT_Set_Pixel_Sizes(face, 0, 48);

    error = FT_Load_Char(face, 'X', FT_LOAD_RENDER);

    if (error) return error;

    return 0; // 初始化成功
}
