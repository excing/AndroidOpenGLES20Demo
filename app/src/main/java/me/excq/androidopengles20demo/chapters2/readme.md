## 绘制一个三角形

主要流程：

0. 设置画布大小
1. 创建一个 gles 执行程序 program
2. 创建一个顶点着色器和片段着色器 shader
3. 添加 shader 到 program
4. 链接 program 到可执行的 gles 处理器上
5. 启用 program 为 gles 的上下文
6. 获取顶点着色器里的顶点变量 vPosition 的句柄 mPositionHandle
7. 设置 mPositionHandle 为当前可编辑
7. 初始化顶点数据（可提前），并通过 mPositionHandle 赋值给 vPosition
8. 设置顶点绘制路径
9. 获取片段着色品里的颜色变量 vColor 的句柄 mColorHandle
10. 初始化颜色数据（可提前），并通过 mColorHandle 赋值给 vColor
11. 设置 mPositionHandle 为禁止修改

整个流程分成三步：

1. 定义着色器
2. 创建和初始化
3. 绘制

### 1. 定义着色器

顶点着色器: vertexShaderCode

```
attribute vec4 vPosition;
void main() {
  gl_Position = vPosition;
};
```

片段着色器: fragmentShaderCode

```
precision mediump float;
uniform vec4 vColor;
void main() {
  gl_FragColor = vColor;
};
```

### 2. 创建和初始化

定义画布大小

```kotlin
GLES20.glViewport(0, 0, width, height)
```

初始化 gles 客户端和着色器

```kotlin
if (0 == mProgram) {
    mProgram = GLES20.glCreateProgram()

    val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
    val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

    GLES20.glShaderSource(vertexShader, vertexShaderCode)
    GLES20.glCompileShader(vertexShader)
    GLES20.glAttachShader(mProgram, vertexShader)

    GLES20.glShaderSource(fragmentShader, fragmentShaderCode)
    GLES20.glCompileShader(fragmentShader)
    GLES20.glAttachShader(mProgram, fragmentShader)

    GLES20.glLinkProgram(mProgram)
}
```

### 3. 绘制

定义顶点和颜色

```kotlin
private val color = floatArrayOf(
    0.63671875f, 0.76953125f, 0.22265625f, 1.0f
)

private val points = floatArrayOf(
    0f, 0.5f, 0.0f,
    -0.5f, -0.5f, 0.0f,
    0.5f, -0.5f, 0.0f
)
```

初始化数据

```kotlin
vertexBuffer = ByteBuffer.allocateDirect(points.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer();
vertexBuffer.put(points)
vertexBuffer.position(0)
```

然后开始绘制

```kotlin
GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
GLES20.glClearColor(r, b, g, a)

GLES20.glUseProgram(mProgram)

val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
GLES20.glEnableVertexAttribArray(mPositionHandle)
GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer)
GLES20.glDrawArrays(GLES20.GL_TRIANGLES,  0, 3)

val mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
GLES20.glUniform4fv(mColorHandle, 1, color, 0)

GLES20.glDisableVertexAttribArray(mPositionHandle)
```
