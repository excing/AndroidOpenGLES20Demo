#include <jni.h>
#include <ft2build.h>
#include FT_FREETYPE_H

#include <android/log.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <string.h>

#define LOG_TAG "chpaters11"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

struct Character {
    GLuint TextureID;
    FT_Int Width;
    FT_Int Height;
    FT_Int Left;
    FT_Int Top;
    FT_Pos Advance;
};

GLfloat surfaceWidth = 0;
GLfloat surfaceHeight = 0;

JNIEXPORT void JNICALL
Java_me_excq_androidopengles20demo_chapters11_MyRenderer02_surfaceChanged(
        JNIEnv *env,
        jobject thiz,
        jint width,
        jint height
) {
    glViewport(0, 0, width, height);

    surfaceWidth = width;
    surfaceHeight = height;
}

int init = 0;
const jchar *textUnicodeString;
const char *fontPathNativeString;

GLuint characterTextureID;
GLuint programID = 0;
GLuint VBO;

JNIEXPORT void JNICALL
Java_me_excq_androidopengles20demo_chapters11_MyRenderer02_init(
        JNIEnv *env,
        jobject thiz,
        jstring font_path,
        jint program_id,
        jstring text
) {
    fontPathNativeString = (*env)->GetStringUTFChars(env, font_path, 0);
    programID = (GLuint) program_id;
    textUnicodeString = (*env)->GetStringChars(env, text, 0);
}

int initTexture() {
    if (init) return -1;

    FT_Face face;
    FT_Library ft;
    FT_Error error;

    error = FT_Init_FreeType(&ft);

    if (error) return NULL;

    error = FT_New_Face(ft, fontPathNativeString, 0, &face);

    if (error) return NULL;

    error = FT_Set_Pixel_Sizes(face, 0, 12 * 12);

    if (error) return NULL;

//    error = FT_Select_Charmap(face, ft_encoding_unicode);

//    if (error) return NULL;

    FT_ULong wChar = textUnicodeString[0];

    error = FT_Load_Char(face, wChar, FT_LOAD_DEFAULT);

    FT_GlyphSlot glyph = face->glyph;
    FT_Render_Glyph(glyph, FT_RENDER_MODE_NORMAL);

    if (error) {
        return NULL;
    }

    glActiveTexture(GL_TEXTURE0);

    GLuint texture;
    glGenTextures(1, &texture);
    glBindTexture(GL_TEXTURE_2D, texture);
    glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA,
            face->glyph->bitmap.width,
            face->glyph->bitmap.rows,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            face->glyph->bitmap.buffer
    );
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    characterTextureID = texture;

    GLfloat xpos = 300 + face->glyph->bitmap_left;
    GLfloat ypos = 500 + (face->glyph->bitmap.rows - face->glyph->bitmap_top);

    GLfloat scale = 5.0f;

    GLfloat w = face->glyph->bitmap.width / surfaceWidth * scale;
    GLfloat h = face->glyph->bitmap.rows * scale / surfaceHeight;

    xpos = (xpos - surfaceWidth) / surfaceWidth;
    ypos = (surfaceHeight - ypos) / surfaceHeight;

    GLfloat vertices[24] = {
            // 第一个三角形
            xpos, ypos, 0.0f, 0.0f,
            xpos, ypos - h, 0.0f, 1.0f,
            xpos + w, ypos - h, 1.0f, 1.0f,

            // 第二个三角形
            xpos, ypos, 0.0f, 0.0f,
            xpos + w, ypos - h, 1.0f, 1.0f,
            xpos + w, ypos, 1.0f, 0.0f
    };

    glGenBuffers(1, &VBO);
    glBindBuffer(GL_ARRAY_BUFFER, VBO);
    glBufferData(GL_ARRAY_BUFFER, sizeof(GLfloat) * 6 * 4, vertices, GL_STATIC_DRAW);
//    glBindBuffer(GL_ARRAY_BUFFER, 0);

    FT_Done_Face(face);
    FT_Done_FreeType(ft);

    init = 1;

    LOGI("INFO::FREETYPE: init success");

    return 0;
}

JNIEXPORT void JNICALL
Java_me_excq_androidopengles20demo_chapters11_MyRenderer02_drawFrame(
        JNIEnv *env,
        jobject thiz
) {
    // initShader(); // 在 Java 层执行该操作。
    initTexture();

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_SRC_ALPHA);
    glClear(GL_COLOR_BUFFER_BIT);
    glClearColor(0.3f, 0.5f, 1.0f, 0.0f);

    glUseProgram(programID);

    glUniform3f(glGetUniformLocation(programID, "ourTextColor"), 0.5, 0.8, 0.2);

    GLuint vertexHandle = (GLuint) glGetAttribLocation(programID, "vertex");
    glEnableVertexAttribArray(vertexHandle);
    glBindBuffer(GL_ARRAY_BUFFER, VBO);
    glVertexAttribPointer(vertexHandle, 4, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), 0);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, characterTextureID);

    glDrawArrays(GL_TRIANGLES, 0, 6);

    glDisableVertexAttribArray(vertexHandle);
}