## 封装着色器

编写、编译和管理着色器是件麻烦事。
在着色器主题的最后，我们写一个类来让我们的生活更轻松一点，他可以从文件中读取着色器，然后编译并链接他们，并进行错误检测。
这也会让你了解该如何封闭目前所学的知识到一个抽象对象中。

我们会把着色器类全部话头文件里，主要是为了学习用途，当然也方便移植。
我们先定义必要的 JavaBean 对象：

```kotlin
data class Shader(var vertexPath: String, var fragmentPath: String) {
    var programID: Int = 0
    fun use() {}
    fun getAttribLocation(name: String): Int {}
}
```

着色器类储存了着色器程序的 ID。他的构造方法需要顶点和片段着色器源代码的文件路径。
这样我们就可以把源码的文本文件储存在硬盘上了。
我们还添加了一个 Use 方法和 getAttribLocation 方法，他其实不那么重要，但是能够显示这个自定义类如何让我们的生活变得轻松（虽然只有一点）。

## 从文件读取

我们使用 kotlin 文件流读取着色器内容，储存到几个 `String` 对象里：

```kotlin
init {
    val vertexCode = vertexStream.readBytes().toString(Charset.defaultCharset())
    val fragmentCode = fragmentStream.readBytes().toString(Charset.defaultCharset())

    vertexStream.close()
    fragmentStream.close()

    println("$vertexCode \n>>>>>>>>>>\n$fragmentCode")
}
```

接下来，我们需要编译和链接着色器。
注意，我们也将检查编译/链接是否失败，如果失败则打印编译时错误，调试的时候这些错误输入会及其重要：

```kotlin
val success: IntBuffer = IntBuffer.allocate(1)

GLES20.glShaderSource(vertexShader, vertexCode)
GLES20.glCompileShader(vertexShader)

GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, success)
if (0 == success[0]) {
    throw IllegalArgumentException(
        "Compile vertex Shader has error: \n" +
                GLES20.glGetShaderInfoLog(
                    vertexShader
                )
    )
}

// 片段着色器类似
...

GLES20.glAttachShader(programID, vertexShader)
GLES20.glAttachShader(programID, fragmentShader)
GLES20.glLinkProgram(programID)

GLES20.glGetProgramiv(programID, GLES20.GL_LINK_STATUS, success)
if (0 == success[0]) {
    throw IllegalArgumentException(
        "Link OpenGL program has error: \n" +
                GLES20.glGetProgramInfoLog(programID)
    )
}

GLES20.glDeleteShader(vertexShader)
GLES20.glDeleteShader(fragmentShader)
```

最后我们也会实现 Use 和 getAttribLocation 方法：

```kotlin
fun use() {
    GLES20.glCreateProgram(programID)
}

fun getAttribLocation(name: String): Int {
    return GLES20.glGetAttribLocation(programID, name)
}
```

PS: 刚刚路由器的 DNS 解析失败了，域名不能正常解析，换成 `114.114.114.114` 和 `8.8.8.8` 之后就好了。

现在我们就写完了一个完整的着色器类。
使用这个着色器类很简单：只要创建一个着色器对象，从那一点开始我们就可以开始使用了：

```kotlin
override fun onDrawFrame(gl: GL10?) {
    initShader()

    ...
}

private fun initShader() {
    if (!this::shader.isInitialized) {
        shader = Shader(
            assets.open("chapters5/vertex.glvs"),
            assets.open("chapters5/fragment.glfs")
        )

        mPositionHandle = shader.getAttribLocation("vPosition")
        mColorHandle = shader.getAttribLocation("vColor")
    }
}
```

我们把顶点和片段着色器储存为两个叫做 `vertex.glvs` 和 `shader.glfs` 的文件。
你可以使用自己喜欢的名字命名着色器文件。

todo 明天把切换 Renderer 实现一下，晚安
