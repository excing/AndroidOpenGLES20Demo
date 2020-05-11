## 贝赛尔曲线

> 转载自：<https://zhuanlan.zhihu.com/p/111611096>

### 什么是贝赛尔曲线

贝赛尔曲线线 1962 年，由法国工程师皮埃尔·贝济埃所广泛发表，
他运用贝赛尔曲线来为汽车的主体进行设计，可以设计出曲线形车身。

贝塞尔曲线主要用于二维图形应用程序中的数学曲线，
曲线主要由起始点，终止点和控制点组成，通过调整控制点，绘制贝塞尔曲线开关则会随之发生变化。
贝塞尔曲线现在已广泛用于计算机图形、动画、字体等，基本上每个现代图形编辑器都支持他。

一些比较常见的一阶、二阶和三阶贝塞尔曲线：

一阶贝塞尔曲线

```
P(t) = (1 - t)*P0 + t*P1
```

二阶贝塞尔曲线

```
P(t) = (1 - t)^2 * P0 + 2t * (1 - t) * P1 + t^2 * P2
```

三阶贝塞尔曲线

```
P(t) = (1 - t)^3 * P0 + 3t * (1 - t)^2 * P1 + 3t^2 * (1 - t) * P2 + t^3 * P3
```

通过上述公式，我们设置好起始点，终止点和控制点，贝塞尔曲线就是由 t 在 [0, 1] 区间对应的无数个点组成。

当然我们实际在设备上绘制时，不可能绘制出无数个点，
一般是根据屏幕像素的大小，对 t 区间进行适当的等间隔插值，再由输出的点组成我们想要的贝塞尔曲线
（此时肉眼分辨不出来两点之间的距离，可以认为他们连成了一条线）。

OpenGL ES 的基本绘制单位是点、线和三角形，既然可以绘制点，只需要基于上述公式计算出点，
然后将其绘制出来，即可得到我们想要的贝塞尔曲线。

以绘制三阶贝塞尔曲线为例，用 GLSL 实现该函数，
然后我们从外部输入一组 t 的取值数组，便可以得到一组对应的用于绘制三阶贝塞尔曲线的点。

```glsl
vec2 bezier_3order(attribute vec2 po, attribute vec2 p1, attribute vec2 p3, attribute float t) {
    float tt = (1.0 - t) * (1.0 - t)
    float ttt = tt * (1.0 - t)

    return ttt * p0 + 3.0 * t * tt * pi + 3 * t * t * (1.0 -t) * p2 + t * t * t * p3
}
```

借助于 GLSL 的内置混合函数 mix，我们可以在用于绘制贝塞尔曲线的点之间进行插值，相当于对上述函数 `bezier_3order` 进行优化：

```glsl
vec2 bezier_3order_mix(in vec2 p0, in vec2 p1, in vec2 p2, in vec2 p3, in float t) {
    vec2 q0 = mix(p0, p1, t);
    vec2 q1 = mix(p1, p2, t);
    vec2 q2 = mix(p2, p3, t);

    vec2 r0 = mix(q0, q1, t);
    vec2 r1 = mix(q1, q2, t);

    return mix(r0, r1, t);
}
```

获取 t 的取值数组，实际上就是对 t 区间 [0, 1] 进行等间取值：

```kotlin
val pointsNum: Int = 256
val pointPerTriangles: Int = 3
val tDataSize = pointsNum * pointPerTriangles
val pointTData = FloatArray(tDataSize)

for (i in 0 until tDataSize step pointPerTriangles) {
    pointTData[i] = i.toFloat() / tDataSize
    pointTData[i + 1] = (i + 1).toFloat() / tDataSize
    pointTData[i + 2] = (i + 2).toFloat() / tDataSize
}

val pointTBuffer = ByteBuffer.allocateDirect(tDataSize * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
pointTBuffer.put(pointTData)
pointTBuffer.position(0)
```

完整的顶点着色器脚本：

```glsl
attribute float a_tData;

uniform vec4 u_StartEndData;
uniform vec4 u_ControlData;

void bezier_3order_mix(in vec2 p0, in vec2 p1, in vec2 p2, in vec2 p3, in float t, out vec2 p) {
    vec2 q0 = mix(p0, p1, t);
    vec2 q1 = mix(p1, p2, t);
    vec2 q2 = mix(p2, p3, t);

    vec2 r0 = mix(q0, q1, t);
    vec2 r1 = mix(q1, q2, t);

    p = mix(r0, r1, t);
}

void main() {
    vec4 pos;
    pos.w = 1.0;

    vec2 p0 = u_StartEndData.xy;
    vec2 p3 = u_StartEndData.zw;

    vec2 p1 = u_ControlData.xy;
    vec2 p2 = u_ControlData.zw;

    float t = a_tData;

    vec2 point;
    bezier_3order_mix(p0, p1, p2, p3, t, point);

    pos.xy = point.xy;

    gl_PointSize = 4.0;
    gl_Position = pos;
}
```

完整的片段着色器脚本：

```glsl
precision mediump float;

uniform vec4 u_Color;

void main() {
    gl_FragColor = u_Color;
}
```

绘制贝塞尔曲线：

```kotlin
val a_tData = GLES20.glGetAttribLocation(programID, "a_tData")
GLES20.glEnableVertexAttribArray(a_tData)
GLES20.glVertexAttribPointer(
    a_tData,
    1,
    GLES20.GL_FLOAT,
    false,
    4,
    0
)

val u_StartEndData = GLES20.glGetUniformLocation(programID, "u_StartEndData")
GLES20.glUniform4f(u_StartEndData, -1.0f, 0.0f, 1.0f, 0.0f)
val u_ControlData = GLES20.glGetUniformLocation(programID, "u_ControlData")
GLES20.glUniform4f(u_ControlData, -0.04f, 0.99f, 0.0f, 0.99f)
val u_Color = GLES20.glGetUniformLocation(programID, "u_Color")
GLES20.glUniform4f(u_Color, 1.0f, 0.3f, 0.0f, 1.0f)

GLES20.glDrawArrays(
    GLES20.GL_POINTS,
    0,
    pointsNum * pointPerTriangles
)
```

源码可见 MyRenderer01

### 绘制动态贝塞尔(MyRenderer02)

一个简单的绘制动态贝塞尔曲线，我们在绘制 y 轴时，随时间而增减，这样一来，我们的贝塞尔曲线就会上下往复的运动了。
我们的顶点着色器主要部分如下：

```glsl
void main() {
    ...
    vec2 p2 = u_ControlData.zw;

    p0.y *= u_Offset;
    p1.y *= u_Offset;
    p2.y *= u_Offset;
    p3.y *= u_Offset;

    float t = a_tData;

    ...
}
```

片段着色器脚本不变，kotlin 绘制代码如下：

```kotlin
override fun onDrawFrame(gl: GL10?) {
    ...

    var offset = (mFrameIndex % 100).toFloat() / 100
    offset = if (1 == (mFrameIndex / 100) % 2) 1 - offset else offset
    val u_Offset = GLES20.glGetUniformLocation(programID, "u_Offset")
    GLES20.glUniform1f(u_Offset, offset)

    ...
}
```

### 绘制曲边扇形(MyRenderer03)

接下来我们基于贝塞尔曲线去绘制曲边扇形（填充曲线与 X 轴之间的区域），则需要 OpenGL 绘制三角形实现，
还要重新输入 t  的取值数组，使得每输出 3 个点包含一个原点，类似于绘制扇形。如下：

```kotlin
val tDataSize = pointsNum * pointPerTriangles
val pointTData = FloatArray(tDataSize)

for (i in 0 until tDataSize step pointPerTriangles) {
    pointTData[i] = i.toFloat() / tDataSize
    pointTData[i + 1] = (i + 3).toFloat() / tDataSize
    pointTData[i + 2] = -1.0f
}

val pointTBuffer = ByteBuffer.allocateDirect(tDataSize * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
pointTBuffer.put(pointTData)
pointTBuffer.position(0)
```

我们的顶点着色器脚本也要改一下：

```glsl
void main() {
    ...
    vec2 point;

    if (t < 0.0) {
        point = vec2(0.0, 0.0);
    } else {
        point = bezier_3order_mix(p0, p1, p2, p3, t);
    }

    ...
}
```

绘制曲边扇形只需要改变绘制模式，将 `GL_POINTS` 改为 `GL_TRIANGLES`。

```kotlin
GLES20.glDrawArrays(
    GLES20.GL_TRIANGLES,
    0,
    pointsNum * pointPerTriangles
)
```

当绘制多个曲线扇形相互叠加时，可以通过混合去产生新的颜色，防止最先绘制的曲边扇形被覆盖。

```kotlin
GLES20.glEnable(GLES20.GL_BLEND)
GLES20.glBlendFuncSeparate(
    GLES20.GL_ONE,
    GLES20.GL_ONE_MINUS_SRC_COLOR,
    GLES20.GL_ONE,
    GLES20.GL_ONE_MINUS_SRC_ALPHA
)
GLES20.glBlendEquationSeparate(GLES20.GL_FUNC_ADD, GLES20.GL_FUNC_ADD)
```

## 画笔

为什么要将画笔功能跟贝塞尔曲线放在一处，
因为由于 Android Touch 事件的时间迟延，因此在快速移动时，点与点之间是不连续的，
实现良好的画笔功能也会用到贝塞尔曲线。

画笔实现从 MyRenderer04 一直到 MyRenderer08，
共展示了一个未经处理的画笔功能（MyRenderer04）以及 3 种实现方式。
他们分别是直线插值和贝塞尔曲线插值以及贝塞尔匀速运动插值。

其中体验最佳的是直线插值，效果最好的是贝塞尔匀速运动插值。

直线插值就是利用三角函数计算两点之间的距离进行均匀的插值。
这种插值方式，优点是快，占用内存小，缺点是效果不好，经常出现我们想要一条曲线，却得到了折线。

贝塞尔插值可以避免这个问题，一般情况下，我们经常采用二阶贝塞尔。
我们的每一个点就是中间的控制点，p0 和 p2 为两点中间的点，这样就构成了一条贝塞尔曲线。

然而贝塞尔插值存在一个问题，即中间密，两端疏，
体现为愈往两端靠近，点愈不连续，可以参见 MyRenderer04 的效果。

引起这个问题的原因，是贝塞尔曲线是不匀速的，
也就是说当时间 t 是匀速前进的时，我们得到的点是不匀速的，
中间的点密，两端的点疏。
为了解决这个问题，出现了贝塞尔曲线匀速运动的解决方案。
为了得到均匀分布的点，求每个点位置的 t。

这个求解过程非常难，最终我放弃了求证，直接将代码抄过来了，期间参考了多位搬运工的源码。

这就是 MyRenderer08，可以看到匀速贝塞尔曲线运动实现的画笔功能。
不过这种实现方式，会占用比较大的内存资源，不知道是我实现过程有问题，
还是这种实现方式本身存在的问题，很容易导致 OOM。
建议在 C 层采用这种实现方式。

MyRenderer06，MRenderer07 是我基于均匀 t 值的一种优化方式。
最终效果还是差了一些，所以就不多写了，有兴趣的可以自行查看。

todo 明天见啊，明天实现文字功能。拜拜，晚安。