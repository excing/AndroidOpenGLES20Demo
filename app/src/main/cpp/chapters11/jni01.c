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

/**
 * 需要用 android log 日志打印时，需要在 Android.mk 文件里添加一句：
 * LOCAL_LDLIBS := -llog
 */
#include <android/log.h>

#define LOG_TAG "chpaters11"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

const char *fontPathNativeString;

JNIEXPORT jint JNICALL
Java_me_excq_androidopengles20demo_chapters11_MyRenderer01_initTreeType(
        JNIEnv *env,
        jobject thiz,
        jstring fontPath) {

    fontPathNativeString = (*env)->GetStringUTFChars(env, fontPath, 0);
    LOGI("font path is %s", fontPathNativeString);

    return 0; // 初始化成功
}

JNIEXPORT jobject JNICALL
Java_me_excq_androidopengles20demo_chapters11_MyRenderer01_getCharBitmap(
        JNIEnv *env,
        jobject thiz,
        jstring s) {

    FT_Face face;
    FT_Library ft;
    FT_Error error;

    error = FT_Init_FreeType(&ft);

    if (error) return NULL;

    error = FT_New_Face(ft, fontPathNativeString, 0, &face);

    if (error) return NULL;

    error = FT_Set_Pixel_Sizes(face, 0, 12 * 12);

    if (error) return NULL;

//    error = FT_Set_Char_Size(face, 0, 16 * 16, 300, 300);

    error = FT_Select_Charmap(face, ft_encoding_unicode);

    FT_CharMap charmap = NULL;

    for (int i = 0; i < face->num_charmaps; i++) {
        charmap = face->charmaps[i];
        LOGI("charmap %d, platform id is %d, encodeing id is %d",
             i,
             charmap->platform_id,
             charmap->encoding_id
        );
    }

    if (error) return NULL;

    /**
     * 获取字符的 unicode 编码。
     *
     * GetStringUTFChars 是获取字符的 UTF 编码
     *
     * 由于 FT_Load_Char 函数默认接受 unicode 编码的字符，
     * 所以此处使用 GetStringChars 方法获得字符
     */
    const jchar *schar = (*env)->GetStringChars(env, s, 0);
    FT_ULong wChar = schar[0];

    /**
     * 通过将 `FT_LOAD_RENDER` 设为加载标记之一，我们告诉 FreeType 去创建一个 8 位的位图， 我们可以通过
     * `face->glyph->bitmap` 来访问这个位图。
     */
    error = FT_Load_Char(face, wChar, FT_LOAD_RENDER);

    //    unsigned int index = FT_Get_Char_Index(face, (FT_ULong) charUnicode);
//    error = FT_Load_Glyph(face, index, FT_LOAD_RENDER);
//    if (0 != error) {
//        return NULL;
//    }
//
//    error = FT_Render_Glyph(face->glyph, FT_RENDER_MODE_NORMAL);

    if (error) {
        return NULL;
    }

    char *className = "me/excq/androidopengles20demo/chapters11/FreeTypeBitmap";
    jclass jniHandle = (*env)->FindClass(env, className);

    if (NULL == jniHandle) return NULL;

    jmethodID constructor = (*env)->GetMethodID(env, jniHandle, "<init>",
                                                "(IIIIILjava/nio/ByteBuffer;)V");
    if (NULL == constructor) return NULL;

    FT_Bitmap bitmap = face->glyph->bitmap;

    unsigned int width = bitmap.width;
    unsigned int height = bitmap.rows;

//    LOGI("font size is %d, %d", width, rows);

//    signed long width = face->glyph->metrics.width;
//    signed long rows = face->glyph->metrics.height;

//    LOGI("font size is %lu, %lu", width1, rows1);

    jobject directBuffer = (*env)->NewDirectByteBuffer(env, bitmap.buffer, height * width);

    jobject jniHandleObject = (*env)->NewObject(
            env,
            jniHandle,
            constructor,
            (jint) width,
            (jint) height,
            (jint) face->glyph->bitmap_left,
            (jint) face->glyph->bitmap_top,
            (jint) face->glyph->advance.x,
            directBuffer
    );

    FT_Done_Face(face);
    FT_Done_FreeType(ft);

    if (NULL == jniHandleObject) return NULL;

    return jniHandleObject;
}

JNIEXPORT void JNICALL
Java_me_excq_androidopengles20demo_chapters11_MyRenderer01_destroyTreeType(
        JNIEnv *env,
        jobject thiz
) {
}
