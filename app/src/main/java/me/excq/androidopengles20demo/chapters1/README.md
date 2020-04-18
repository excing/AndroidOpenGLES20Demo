### 1. 申请权限

```xml
    <!--告诉系统这个 APP 需要 OpenGL ES 2.0-->
    <uses-feature
        android:glEsVersion="0x00020000"
        android:required="true" />

    <!--申明需要纹理压缩-->
    <supports-gl-texture android:name="GL_OES_compressed_ETC1_RGB8_texture" />
    <supports-gl-texture android:name="GL_OES_compressed_paletted_texture" />
```

### 2. 使用 GLSurfaceView 绘制 OpenGL ES 图形

1. 表明使用 es 2.0 客户端版本

```kotlin
glSurfaceView.setEGLContextClientVersion(2)
```

2. 设置 Renderer

```kotlin
glSurfaceView.setRenderer(myRenderer)
// 连续渲染模式，每隔 16ms 渲染一次（60fps），默认为此模式。
glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY)
// 不连续模式，需要用户调用 GLSurfaceView.requestRender() 函数时渲染。
glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY)
```

3. 实现 `GLSurfaceView.Renderer` 接口

`GLSurfaceView.Renderer` 接口包含三个函数：

- onDrawFrame: 每帧画面
- onSurfaceChanged: 画布大小发生改变
- onSurfaceCreated: 画布已创建成功

我们的主要工作，就是围绕这三个函数展开的，其中主要是 onDrawFrame 函数

### 实验

1. 点击屏幕，背景颜色随机变换

主要代码段：

```kotlin
        myRenderer = MyRenderer(1f, 1f, 1f, 1f)
        glSurfaceView.setRenderer(myRenderer)

        // ...

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(r, g, b, a);
        }

        // ...

        private fun touchUp() {
            val random = Random(System.currentTimeMillis())
            renderer.r = random.nextFloat()
            renderer.g = random.nextFloat()
            renderer.b = random.nextFloat()
            renderer.a = random.nextFloat()
        }
```

2. 测试 setRenderMode 方法

```kotlin
        myRenderer = MyRenderer(1f, 1f, 1f, 1f)
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        glSurfaceView.setRenderer(myRenderer)

        // ...

        override fun onDrawFrame(gl: GL10?) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(r, g, b, a);
        }

        // ...

        private fun touchUp() {
            val random = Random(System.currentTimeMillis())
            renderer.r = random.nextFloat()
            renderer.g = random.nextFloat()
            renderer.b = random.nextFloat()
            renderer.a = random.nextFloat()

            glSurfaceView.requestRender()
        }
```
