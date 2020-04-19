## 着色器

着色器（Shader）是运行在 GPU 上的小程序。
这些小程序为图形渲染管线的某个特定部分而运行。
从基本意义上来说，着色器只是一种把输入转化为输出的程序。
也是一种非常独立的程序，因为他们之间不能相互通信，他们唯一的沟通只有通过输入和输出。

> 这有点类似黑匣子，只有输入输出。

## GLSL

着色器是使用一种叫 GLSL 的类 C 语言写成的。GLSL 是为图形计算量身定制的，它包含一些针对向量和矩阵操作的有用特性。

着色器的开关总是要声明版本，接着是输入和输出变量、uniform 和 main 函数。
每个着色器的入口点都是 main 函数，在这个函数中我们处理所有的输入变量，并将结果输出到变量中。

一个典型的着色器有下面的结构：

```glsl
#version version_number

in type in_variable_name1;
in type in_variable_name2;

out type out_variable_name1;

uniform type uniform_name1;

int main() {
  // 处理输入并进行一些图形操作
  ...
  // 输出处理过的结题以输出变量
  out_variable_name1 = weird_stuff_we_processed;
};
```

> 以上着色器语法是 OpenGL 3.0，OpenGL 2.0 的语言略有不同。
> 关键字 `in` 在 2.0 中使用的是 `attribute`
> 关键字 `out` 在 2.0 中使用的是 `varying`
> 在 3.0 中还添加 `layout` 关键字，可直接为脚本中的属性指定位置，无需要在程序中使用 `GLES20.glGetAttribLocation` 获取

当我们特别谈论到顶点着色器的时候，每个输入变量也叫**顶点属性**（Vertex Attribute）。
我们能声明的顶点属性是有上限的，它一般由硬件决定。
OpenGL 确保至少有 16 个包含 4 分量的顶点属性可用，但有些硬件或许允许更多的顶点属性，
你可以查询 `GL_MAX_VERTEX_ATTRIBS` 来获取具体的上限：

```kotlin
val params = IntBuffer.allocate(4)
GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, params)
println("GLES20: params: " + params[0])
```

输出：

```log
2020-04-19 15:26:27.399 8770-8841/me.excq.androidopengles20demo I/System.out: GLES20: params: 16
```

通常情况下它至少会返回 16 个，大部分情况下是够用了。

## 数据类型

和其他编程语言一样，GLSL 有数据类型可以来指定变量的各类。
GLSL 中包含 C 等其他语言大部分的默认基础数据类型：`int`、`float`、`double`、`uint` 和 `bool`。
GLSL 也有两种容器类型，它们会在这个数据中使用很多，分别是向量（Vector）和矩阵（Matrix）其中矩阵会在后面再讨论。

### 向量

GLSL 中的向量是一个可以包含有 1、2、3 或者 4 个分量的容器，分量的类型可以是前面默认基础的任意一个。
他们可以是下面的形成（`n` 代表分量的数量）：

类型 | 含义
---- | -----
`vecn` | 包含 `n` 个 float 分量的默认向量
`bvecn` | 包含 `n` 个 b
`ivecn` | 包含 `n` 个 int 分量的向量
`uvecn` | 包含 `n` 个 unsigned int 分量的向量
`dvecn` | 包含 `n` 个 double 分量的向量

大多数时候我们使用 `vecn`，因为 float 足够满足大多数要求了。

一个向量的分量可以通过 `vec.x` 这种方式获取，这里 `x` 是指这个向量的第一个分量。
你可以分别使用 `.x`、`.y`、`.z` 和 `.w` 来获取他们的第 1、2、3、4 个分量（变量）。
GLSL 也允许你对颜色使用 `rgba`，或者是对纹理坐标使用 `stpq` 访问相同的分量。

```glsl
attribute vec4 vPosition;
void main() {
  gl_Position = vec4(vPosition.x, vPosition.y, vPosition.z, vPosition.w);
};
```

向量这一数据类型也允许一些有趣而灵活的分量选择方式，叫做 **重组**（Swizzling）。
重组允许这样的语法：

```glsl
vec2 someVec;
vec4 differentVec = someVec.xyyx;
vec3 anotherVec = differentVec.zyw;
vec4 otherVec = someVec.xxxx + anotherVec.yxzy;
```

你可以使用上面 4 个字母任意组合来创建一个和原来向量一样长的（同类型）新向量，只要原来向量有那些分量即可。
然而，不允许在一个 `vec2` 向量中去获取 `.z` 元素。
我们也可以把一个向量作为一个参数传给不同的向量构造函数，以减少需求参数的数量：

```glsl
vec2 vect = vec2(0.5f, 0.7f)
vec4 result = vec4(vect, 0.0f, 0.1f);
vec4 otherResult = vec4(result.xyz, 1.0f);
```

向量是一种灵活的数据类型，我们可以用在各种输入和输出上。

## 输入与输出

虽然着色器是各自独立的小程序，但是他们都是一个整体的一部分，出于这样的原因，我们希望每个着色器都有输入和输出，这样才能进行数据交流和传递。
GLSL 定义了 `in` 和 `out` 关键字专门来实现这个目的。
每个着色器使用这两个关键字设定输入和输出，只要一个输出变量与下一个着色器阶段的输入匹配，他就会传递下去。
但在顶点和片段着色器中会有点不同。

> `in` 和 `out` 是 OpenGL 3.0 的语法，OpenGL 2.0 使用 `attribute` 和 `varying`

顶点着色器应该接收的是一种特殊形式的输入，否则就会效率低下。
顶点着色器的输入特殊在，他从顶点数据中直接接收输入。
为了定义顶点数据该如何管理，我们使用 `location` 这一元数据指定输入变量，这样我们才可以在 CPU 上配置顶点属性。

> 这个是 OpenGL 3.0 的特性，在 OpenGL 2.0 中没有，需要在程序中使用 `GLES20.glGetAttribLocation` 方法获取。

另一个例外是片段着色器，他需要一个 `vec4` 颜色输出变量，因为片段着色器需要生成一个最终输出的颜色。
如果你在片段着色器没有定义输出颜色，OpenGL 会把你的物体渲染为黑色（或白色）。

所以，如果我们打算从一个着色器向另一个着色器发送数据，我们必须在发送方着色器中声明一个输出，在接收方着色器中声明一个类似的输入。
当类型和名字都一样的明个，OpenGL 就会把两个变量链接到一起，他们之间就能发送数据了（这是在链接程序对象时完成的）。
如果代码段展示了如何链接数据。

#### OpenGL 3.0 版

**顶点着色器**：

```glsl
#version 330 core

layout (location = 0) in vec3 position;

out vec4 vertexColor;

void main() {
  gl_Position = vec4(position, 1.0f);
  vertexColor = vec4(0.5f, 0.0f, 0.0f, 1.0f);
}
```

**片段着色器**:

```glsl
#version 330 core
in vec4 vertexColor;

out vec4 color;

void main() {
  color = vertexColor;
}
```

#### OpenGL 2.0 版

**顶点着色器**：

```glsl
#version 200 core

attribute vec3 position;

varying vec4 vertexColor;

void main() {
  gl_Position = vec4(position, 1.0f);
  vertexColor = vec4(0.5, 0.0, 0.0, 1.0);
}
```

**片段着色器**:

```glsl
#version 200 core

varying vec4 vertexColor;

void main() {
  gl_FragColor = vertexColor;
}
```

可以看到我们在顶点着色器中声明了一个 `vertexColor` 作为 `vec4` 输出，并在片段着色器中声明了一个类似的 `vertexColor`。
由于他们名字相同且类型相同，片段着色器中的 `vertexColor` 就和顶点着色器中的 `vertexColor` 链接了。
由于我们在顶点着色器中将颜色设置为深红色，最终的片段也是深红色的。

我们成功地从顶点着色器向片段着色器发送了数据。

## Uniform

**Uniform** 是一种从 CPU 中的应用向 GPU 中的着色器发送数据的方式，但 uniform 和顶点属性有点不同。
首先，uniform 是**全局的**（Global）。
全局意味着 unfiorm 变量必须在每个着色器程序对象中都是独一无二的，而且他可以被着色器程序的任意着色器在任意阶段访问。
第二，无论你把 uniform 值设置成什么，uniform 会一直保存他们的数据，直到他们被重置或更新。
也就是说，着色器程序无法更新 uniform 的值，只能通过应用程序传递值给 uniform 变量。

我们可以在一个着色器中添加 `uniform` 关键字至类型和变量名前来声明一个 GLSL 的 uniform。
从此处开始就可以在着色器中使用新声明的 uniform 了。
我们来试试能否通过 uniform 设置三角形的颜色（废话嘛，chapters2 里就用了这招了）：

**片段着色器**

```glsl
#version 200 core;

out vec4 color;

uniform vec4 vertexColor;

void main() {
  out = vertexColor;
}
```

> OpenGL 2.0 略有不同，没有 `out vec4 color` 一行，且 `out` 变量由内置变量 `gl_FragColor` 替代。

我们在片段着色器中声明了一个 `uniform vec4` 的 `vertexColor`，并把片段着色器的输出颜色设置为 uniform 值的内容。
因为 uniform 是全局变量，我们可以在任何着色器中定义他们，而无需要通过顶点着色器作为中介。
顶点着色器中不需要这个 uniform，所以我们不用在那里定义他。

> 如果声明了一个 uniform 却在 GLSL 代码中没有用过，编译器会静默移除这个变量，
> 导致最后编译出的版本中并不会包含他，这可能会导致几个非常麻烦的错误。

这个 uniform 现在还是空的，我们还没有给他添加任何数据，所以下面我们就做这件事。
首先需要找到着色器中 uniform 属性的索引/位置值。
得到这个索引/位置值后，就可以更新他的值了。
这次让他随着时间改变颜色：

```kotlin
val colors = floatArrayOf(
    0.5f, 0.0f, 0.0f, 1.0f
)
colors[2] = ((sin(System.currentTimeMillis().toDouble()) + 1) / 2).toFloat()
val mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
GLES20.glUseProgram(mProgram)
GLES20.glUniform4fv(mColorHandle, 1, colors, 0)
```

我们通过 `System.currentTimeMillis()` 获取当前系统时间。
然后使用 `sin` 陈洁灵让颜色在 `0.0` 到 `1.0` 之间改变，最后将结果储存到 `colors` 里。

接着，我们用 `glGetUniformLocation` 查询 `vColor` 的位置。
我们为查询函数提供着色器程序和 unifrom 的名字（这是我们希望获得的位置的来源）。
如果 `glGetUniformLocation` 返回 `-1` 就代表没有找到这个位置。
最后我们通过 `glUniform4fv` 方法设置 uniform 值。
注意，查询 uniform 地址不要求之前使用过着色器程序，
但更新一个 uniform 之前必须使用程序，即调用 `glUseProgram`，因为这是在当前激活的着色器程序中设置 uniform 值的。

OpenGL 不支持类型重载，在方法参数不同的时候，就要为其定义新的方法，`glUseProgram` 就是一个典型的例子。
这个函数有一个特定的后缀，标识设定的 uniform 的类型，可能有的后缀有：

后缀 | 含义
---- | ----
`f` | 方法需要 1 个 float 作为他的值
`i` | 方法需要 1 个 int 作为他的值
`ui` | 方法需要 1 个 unsigned int 作为他的值
`3f` | 方法需要 3 个 float 作为他的值
`fv` | 方法需要 1 个 float 向量/数组作为他的值

每当你打算配置一个 OpenGL 的选项时，就可以简单地根据这些规则选择适合你的数据类型的重载方法。
在我们的例子里，我们希望设定 uniform 的 1 个 float 向量（rbga），所以我们通过 `glUniform4fv` 传递我们的数据。
注意，我们也可以使用 `4f` 版本。

可以看到 uniform 对于设置一个在渲染迭代中会改变的属性是一个非常有用的工具，他也是一个程序和着色器间数据交互的很好工具。
但假如我们打算为每个顶点设置一个颜色的时候该怎么？
这种情况下，我们不得不声明和顶点数目一样多的 uniform 了。
在这一问题上更好的解决方案是在顶点属性中包含更多的数据，这是接下来要做的事情。

## 更多属性

todo VBO、VAO

我们把颜色数据加进顶点数据中。
我们把颜色数据添加为 3 个 float 值至 points 数组，三个顶点分别指定为红色、绿色和蓝色：

```kotlin
val points = floatArrayOf(
     0.0f,  0.5f, 0.0f, 1.0f, 0.0f, 0.0f,
    -0.5f, -0.5f, 0.0f, 0.0f, 1.0f, 0.0f,
     0.5f, -0.5f, 0.0f, 0.0f, 0.0f, 1.0f
)
```

由于现在有更多的数据要发送到顶点着色器，我们有必要调整一下顶点着色器，使它能够接收颜色值作为一个顶点属性输入。

这里先补充一下 VBO、VAO、EBO 的信息。

### 使用 VBO 绘制。

VBO（Vertex Buffer Object）是指顶点缓冲区对象，而 EBO（Element Buffer Object）是指图元索引缓冲区对象。
VAO 和 EBO 实际上是对同一类 Buffer 按照用途的不同称呼。

OpenGL ES 编程中，用于绘制的顶点数组数据首先保存在 CPU 内存中，
在调用 `glDrawArrays` 或者 `glDrawElements` 等进行绘制时，
需要将顶点数组数据从 CPU 内存拷贝到 GPU 内存（显存）

我们不是每次都要去进行内存拷贝，比如一些固定的数组数据，
如果可以在显存中缓存这些数据，就可以在很大程度上降低内存拷贝带来的开销。

VBO 和 EBO 的出现就是为了解决这个问题。
（有一说：VBO 和 EBO 是 OpenGL ES 3.0 的特性，但在 Android 上，OpenGL ES 2.0 也可以用。）
VBO 和 EBO 的作用是在显存中提前开辟好一块内存，用于缓存顶点数据或者图元索引数据，
从而避免每次䌊时的 CPU 与 GPU 之间的内存拷贝，可以改进渲染性能，降低内存带宽和功耗。

创建和更新 VBO 的代码如下：

```kotlin
val vbo = IntBuffer.allocate(1)
GLES20.glGenBuffers(1, vbo)
GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])

// @see https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glBufferData.xml
GLES20.glBufferData(
    GLES20.GL_ARRAY_BUFFER,
    points.size * 4,
    vertexBuffer,
    GLES20.GL_STATIC_DRAW
)
```

有三步，缺一不可：

1. `glGenBuffers`：创建一个 VBO
2. `glBindBuffer`：为该 VBO ID (即 `vbo[0]`) 指定一个缓存类型，`GL_ARRAY_BUFFER` 标志指定的缓存区对象用于保存顶点数组。当该 ID 为 `0` 时，该缓存类型所表示的缓存区对象将被解绑，即第 3 步中的缓存对象不可用。
3. `glBufferData`：为指定的缓存类型设置一个用于指定用途的缓存区对象，此处的缓存类型需与第二步的缓存类型保存一致，`GL_STATIC_DRAW` 表示该缓存区对象的用途是绘制。

使用 VBO 的代码如下：

```kotlin
GLES20.glEnableVertexAttribArray(mPositionHandle)
GLES20.glVertexAttribPointer(
    mPositionHandle,
    3,
    GLES20.GL_FLOAT,
    false,
    24,
    0
)
```

首先启用着色器中的输入可操作，然后为顶点赋值，`glVertexAttribPointer` 与之前的同名方法中的最后一个参数不同。
此处参数的值，是 vbo 所绑定的缓存中的索引值，表示该属性的第一个值所有缓存位置。

当我们将顶点位置信息和颜色信息写入一个数组数据中时，使用 VBO 绘制的代码如下：

```kotlin
GLES20.glEnableVertexAttribArray(mPositionHandle)
GLES20.glVertexAttribPointer(
    mPositionHandle,
    3,
    GLES20.GL_FLOAT,
    false,
    24,
    0
)

GLES20.glEnableVertexAttribArray(mColorHandle)
GLES20.glVertexAttribPointer(
    mColorHandle, 3,
    GLES20.GL_FLOAT,
    false,
    24,
    12
)
```

前两段，设置顶点位置，后两段，设置顶点颜色，其中 `12` 就是第一个颜色值所处缓存中的偏移量。
