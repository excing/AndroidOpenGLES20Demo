## 认识 VAO、VBO 和 EBO

[chapters 3](https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/chapters3) 简单的介绍了 VBO、VAO 和 EBO，并对 VBO 进行了简单的运用。

简单的说，这三个都是缓冲区对象，按不同用途的不同称呼。

VBO 缓冲的一般是顶点属性数据，EBO 缓冲的是图元索引数据，即顶点绘制顺序，VAO 缓冲的是 VBO 的操作过程，具体请看代码：

### VBO 的创建和使用 (chapters4#MyRenderer01)

```kotlin
private fun initBuffer() {
    if (null == vbo) {
        vbo = IntBuffer.allocate(1)
        GLES20.glGenBuffers(1, vbo)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo!![0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            points.size * 4,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW
        )
    }
}

override fun onDrawFrame(gl: GL10?) {
    GLES20.glEnableVertexAttribArray(mPositionHandle)
    GLES20.glVertexAttribPointer(
        mPositionHandle,
        3,
        GLES20.GL_FLOAT,
        false,
        24,
        0
    )
}
```

### EBO 的创建和使用 (chapters4#MyRenderer02)

```kotlin
private fun initBuffer() {
    if (null == boIDs) {
        boIDs = IntBuffer.allocate(2)
        GLES20.glGenBuffers(2, boIDs)

        // 绑定第一个缓冲区对象，当缓存类型为 `GL_ARRAY_BUFFER` 时，该对象称之为 VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, boIDs!![0])
        ...

        // 绑定第二个缓冲区对象，当缓存类型为 `GL_ELEMENT_ARRAY_BUFFER` 时，该对象称之为 EBO
        // EBO 是图元索引缓存区对象，用于按该索引顺序绘制图元，
        // 因此不能使用 `glDrawArrays` 绘制图元，应使用 `glDrawElements` 方法
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, boIDs!![1])
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            indices.size * 2,
            indicesBuffer,
            GLES20.GL_STATIC_DRAW
        )
    }
}

override fun onDrawFrame(g1: GL10?) {
    ...

    GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, 0)
}
```

### VAO 的创建和使用

OpenGL ES 2.0 中不支持 VAO。

## 实验

### 使用 VBO 和 EBO 绘制一个正方形 (chapters4#MyRenderer03)

我们只需要将顶点属性的数据修改一下就可以了，如下：

```kotlin
private val points = floatArrayOf(
    // 坐标            // 颜色
    0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
    0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
    -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
    -0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 1.0f
)

// 顶点绘制顺序
private val indices = shortArrayOf(
    0, 1, 2, 0, 2, 3
)
```

最终效果就是一个正方形。

### 绘制多个几何体 (chapters4#MyRenderer04)

```kotlin
private val points = floatArrayOf(
    // 坐标            // 颜色

    // 第一个方形坐标和颜色
    0.5f, 0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
    0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f,
    -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
    -0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 1.0f,

    // 第二个方形坐标和颜色
    0.2f, 0.2f, 0.0f, 1.0f, 0.0f, 0.0f,
    0.2f, -0.2f, 0.0f, 0.0f, 0.0f, 1.0f,
    -0.2f, -0.2f, 0.0f, 0.0f, 1.0f, 0.0f,
    -0.2f, 0.2f, 0.0f, 0.5f, 0.5f, 1.0f
)

// 顶点绘制顺序
private val indices = shortArrayOf(
    // 第一个方形的绘制顺序
    0, 1, 2, 0, 2, 3,

    // 第二个方形的绘制顺序
    4, 5, 6, 4, 6, 7
)

override fun onDrawFrame(gl: GL10?) {
    ...

    // 这里是绘制第一个方形
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES,
        6,
        GLES20.GL_UNSIGNED_SHORT,
        0
    )
    // 这里是绘制第二个方形
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES,
        6,
        GLES20.GL_UNSIGNED_SHORT,
        12
    )

    ...
}
```

`onDrawFrame` 方法里的绘制还可以写成一条语句：

```kotlin
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES,
        12,
        GLES20.GL_UNSIGNED_SHORT,
        0
    )
```

即一次绘制完成，其他代码与之前无异。
