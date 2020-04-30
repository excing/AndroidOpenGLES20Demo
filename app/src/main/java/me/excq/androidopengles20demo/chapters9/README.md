## 摄像机

前面的教程中我们讨论了观察矩阵以及如何使用观察矩阵移动的方式来模拟出摄像机，产生一种我们在移动的感觉，而不是场景在移动。

本节我们将会讨论如何在 OpenGL 中配置一个摄像机，并且将会讨论 FPS 风格的摄像机，让你能够在 3D 场景中自由移动。
我们也会讨论键盘和鼠标输入，最终完成一个自定义的摄像机类。

## 摄像机/观察空间

当我们讨论摄像机/观察空间（Camera/View Space）的时候，是在讨论以摄像机的视角作为场景原点时，场景中所有的顶点坐标。
观察矩阵把所有的世界坐标变换为相对于摄像机位置与方向的观察坐标。
要定义一个摄像机，我们需要他在世界空间中的位置、观察的方向、一个指向他右侧的向量以及一个指定他上方的向量。

### 1、摄像机位置

获取摄像机位置很简单。
摄像机位置简单来说就是世界空间中一个指定摄像机位转换向量。
我们把摄像机位黑市黑上一节中的那个相同的位置：

```kotlin
private val cameraPos = floatArrayOf(
    0.0f, 0.0f, 3.0f
)
```

> 不要忘记正 Z 轴是从屏幕指向你的，如果我们希望摄像机向后移动，我们就沿着 Z 轴的正方向移动。

### 2、摄像机方向、右轴、上轴

下一个需要的向量是摄像机的方向，这里指的是摄像机指向哪个方向。
现在我们让摄像机指向场景原点：（0, 0, 0）。
还记得如果将两个矢量相减，我们就能得到这两个矢量的差吗？
用场景原点向量减去摄像机位置向量的结果就是摄像机的指向向量。
由于我们知道摄像机指向 Z 轴负方向（屏幕里面），但我们希望方向向量（Direction Vector）指向摄像机正方向。
如果我们交换相减的顺序，我们就会获得一个指向摄像机正 Z 轴方向的向量：

此部分见[LearnOpenGL CN 摄像机](https://learnopengl-cn.github.io/01%20Getting%20started/09%20Camera/)一节。

## Look At（MyRenderer01）

使用矩阵的好处之一是如果你使用 3 个相互垂直（或非线性）的轴定义了一个坐标空间，你可以用这 3 个轴外加一个平衡向量来创建一个矩阵，
并且你可以用这个矩阵乘以任何向量来将其变换到那个坐标空间。
这正是 **LookAt** 矩阵所做的，现在我们有了 3 个垂直的轴和一个定义摄像机窗间的位置坐标，我们可以创建我们自己的 LookAt 矩阵了。

在 Android 中，我们可以使用 Matrix 提供的方法创建一个 LookAt 矩阵。
我们要做的只是定义一个摄像机位置，一个目标位置和一个表示世界空间中的上向量的向量（我们计算右向量的那个上向量）。
接着 Matrix 就会创建一个 LookAt 矩阵，我们可以把他当作我们的观察矩阵：

```kotlin
Matrix.setIdentityM(view, 0)
Matrix.setLookAtM(
    view,
    0,
    cameraPos[0], cameraPos[1], cameraPos[2],
    0.0f, 0.0f, 0.0f,
    0.0f, 1.0f, 0.0f
)
```

`Matrix.setLookAtM` 方法需要一个位置、目标和上向量。
他会创建一个和在上一节使用的一样的观察矩阵。

在讨论用户输入之前，我们先来做些有意思的事，所我们的摄像机在场景中旋转。
我们会将摄像机的注视点保持在 （0, 0, 0）。

我们需要乃至一点三角学的知识来在每一帧创建一个 x 和 y 坐标，他会代表圆上的一点，我们将会使用他作为摄像机的位置。
通过重新计算 x 和 y 坐标，我们会遍历圆上的所有点，这样摄像机就会绕着场景旋转了。
我们预先定义这个圆的半径 `radius`，在每次渲染迭代中使用渲染次数的方法重新创建观察矩阵，来扩大这个圆：

```kotlin
// val time = 0.0
val radius = 10.0f
time += 0.01f
val camX = sin(time) * radius
val camZ = cos(time) * radius
Matrix.setLookAtM(
    view,
    0,
    camX.toFloat(), 0.0f, camZ.toFloat(),
    0.0f, 0.0f, 0.0f,
    0.0f, 1.0f, 0.0f
)
GLES20.glUniformMatrix4fv(mViewHandle, 1, false, view, 0)
```

如果你运行代码，应该会得到下面的结果：

todo 此处应有效果图（视频）

通过这一小段代码，摄像机现在会随着时间流逝围绕场景转动了。
自己试试改变半径和位置/方向参数，看看 LookAt 矩阵是如何工作的。
同时，如果你哪里卡住的话，可以参考 MyRenderer02 的源码。

## 自由移动（MyRenderer03）

让摄像机绕着场景转抑郁很有趣，但是让我们自己移动摄像机会更有趣！
首先我们必须设置一个摄像机系统，所以在我们的程序前面定义一些摄像机变是很有有用：

```kotlin
private val cameraPos = floatArrayOf(0.0f, 0.0f, 3.0f)
private val cameraFront = floatArrayOf(0.0f, 0.0f, -1.0f)
private val cameraUp = floatArrayOf(0.0f, 1.0f, 0.0f)
```

`setLookAtM` 方法现在成了：

```kotlin
Matrix.setLookAtM(
    view,
    0,
    // 摄像机位置
    cameraPos[0],
    cameraPos[1],
    cameraPos[2],

    // 摄像机指向位置
    cameraFront[0] + cameraPos[0],
    cameraFront[1] + cameraPos[1],
    cameraFront[2] + cameraPos[2],

    // 摄像机的上向量，用于确定 X 轴的
    cameraUp[0],
    cameraUp[1],
    cameraUp[2]
)
```

我们首先将摄像机位置设置为之前定义的 cameraPos。
方向是当前的位置加上我们刚铡定义的方向向量。
这样能保证无论我们怎么移动，摄像机都会注视着目标方向。
让我们摆弄一下这些向量，在按下某些按钮时更新 `cameraPos` 向量。

我们已经修改了 `OnTouchListener` 的回调实现，现在我们来为上下左右滑动添加几个命令：

```kotlin
private val cameraSpeed = 0.05f
private var oldx = 0.0f
private var oldy = 0.0f

/**
 * 坐标轴, 1 表示横向, 2 表示纵向
 */
private var dir = 0

override fun onTouch(action: Int, x: Float, y: Float) {
    when (action) {
        MotionEvent.ACTION_DOWN -> {
            oldx = x
            oldy = y
            dir = 0
        }
        MotionEvent.ACTION_MOVE -> {
            if (0 == dir && 30 < abs(oldx - x)) {
                dir = 1
            } else if (0 == dir && 30 < abs(oldy - y)) {
                dir = 2
            }

            when (dir) {
                1 -> { // 横向滑动
                    val temp = multi(cross(cameraFront, cameraUp), cameraSpeed)
                    if (oldx < x) {
                        cameraPos[0] -= temp[0]
                        cameraPos[1] -= temp[1]
                        cameraPos[2] -= temp[2]
                    } else {
                        cameraPos[0] += temp[0]
                        cameraPos[1] += temp[1]
                        cameraPos[2] += temp[2]
                    }
                }
                2 -> { // 纵向滑动
                    val temp = multi(cameraFront, cameraSpeed)
                    if (oldy < y) {
                        cameraPos[0] += temp[0]
                        cameraPos[1] += temp[1]
                        cameraPos[2] += temp[2]
                    } else {
                        cameraPos[0] -= temp[0]
                        cameraPos[1] -= temp[1]
                        cameraPos[2] -= temp[2]
                    }
                }
            }
        }
    }
}

private fun cross(ai: FloatArray, ar: FloatArray): FloatArray {
    val result = FloatArray(3)

    result[0] = ai[1] * ar[2] - ai[2] * ar[1]
    result[1] = ai[2] * ar[0] - ai[0] * ar[2]
    result[2] = ai[0] * ar[1] - ai[2] * ar[0]

    return result
}

private fun multi(arr: FloatArray, b: Float): FloatArray {
    val result = FloatArray(3)

    result[0] = arr[0] * b
    result[1] = arr[1] * b
    result[2] = arr[2] * b

    return result
}
```

当我们滑动屏幕时，摄像机的位置都会相应更新。
如果我们希望向前或向后移动，我们就向上或向下滑动，
如果我们希望向左或向右移动，我们使用叉乘来创建一个**右向量**，并沿着他相应移动就可以了。
这样就创建了使用摄像机时熟悉的**横移**（Strafe）效果。

todo 没有做标准化处理

现在你就可以移动摄像机了。不过移动速度和系统有关，你可能会需要调整一下 `cameraSpeed`。

## 移动速度

目前物镜的移动速度是个常量。
理论上没什么问题，但是实际情况下根据处理器的能力不同，有些人可能会比其他人每秒绘制更多帧，
也就是以更高的频率调用 `onTouch` 方法。
结果就是根据配置的不同，有些人可能移动很快，而有些人会移动很慢。
当你发布你的程序的时候，你必须确保他在所有硬件和平台上移动速度都一样。

图形程序和游戏通常会跟踪一个**时间差**（DeltaTime）变量，他储存了渲染上一帧所用的时间。
我们把所有速度都去乘以 `deltaTime` 值。
结果就是，如果我们的 `deltaTime` 很大，就意味着上帧的渲染花费了更多时间，
所以这一帧的速度需要变得更高来平衡渲染所花去的时间。
使用这种方法时，无论你的电脑快还是慢，摄像机的速度都会相应平衡，这样每个用户的体验就都一样了。

我们跟踪两个全局变量来计算出 `deltaTime` 值：

```kotlin
private var deltaTime = 0L
private var lastTime = 0L
```

在每一帧中我们计算出新的 `deltaTime` 以备后用：

```kotlin
override fun onDrawFrame(gl: GL10?) {
    val currentTime = System.currentTimeMillis()
    deltaTime = currentTime - lastTime
    lastTime = currentTime
    ...
}
```

现在我们有了 `deltaTime`，在计算速度的时候可以将其考虑进去了：

```kotlin
override fun onTouch(action: Int, x: Float, y: Float) {
    val cameraSpeed = 0.005f * deltaTime
    ...
}
```

与前面的部分结合在一起，我们有了一个更流畅，且在不同硬件上拥有相同速度的摄像机系统：

todo 此处应有效果图（视频）

如果你卡住了，查看一下 MyRenderer04 的源码。

## 视角移动

只能平移没什么意思，特别是我们还不能转身，移动很爱限制。

为了能够改变视角，我们需要改变 `cameraFront` 向量。
然而根据鼠标移动改变方向向量有点复杂，需要一些三角学知识。
如果你对三角学知之甚少，别担心，你可以跳过这一部分，直接复制粘贴代码。
当你想了解更多的时候再回来看。

### 欧拉角

欧拉角（Euler Angle）是可以表示 3D 空间中任何旋转的 3 个值，由莱昂哈德·欧拉（Leonhard Euler）在 18 世纪提出。
一共有 3 种欧拉角：俯仰角（Pitch）、偏航角（Yaw）和滚转角（Roll）：

**俯仰角**是描述我们如何往上或往下看的角。
**偏航角**，表示我们往左和往右看的程序。
**滚转角**代表我们如何**翻滚**摄像机，通常在太空飞船的摄像机中使用。
每个欧拉角都最有一个值表示，把三个角结合起来，我们就能够计算 3D 空间中任何的旋转向量了。

对于我们的摄像机系统来说，我们只关心俯仰角和偏航角，所以我们不会讨论滚转角。
给定一个俯仰角和偏航角，我们可以把他们转换为一个代表新的方向向量的 3D 向量：

```kotlin
val x = (cos(Math.toRadians(pitch)) * cos(Math.toRadians(yaw))).toFloat()
val y = sin(Math.toRadians(pitch)).toFloat()
val z = (cos(Math.toRadians(pitch)) * sin(Math.toRadians(yaw))).toFloat()
```

以上，`pitch` 表示俯仰角，`yaw` 表示偏航角。

## 鼠标输入

我们在 `MyRenderer03` 和 `MyRenderer04` 中，使用 touch 事件，实现了摄像机的平移和前后移动。
本节中，我们继续使用 touch 事件，实现摄像机视角的移动。

首先，先初始化俯仰角和偏航角

```kotlin
private val sensitivity = 0.05f
private var yaw = -90.0
private var pitch = 0.0
```

`sensitivity` 表示移动灵敏度。
如果我们忽略这个值，鼠标移动就会太大了；你可以自己实验一下，找到适合自己的灵敏度值。
`yaw` 表示偏航角，`-90.0` 表示偏航被初始化为 -90.0 度，如果偏航为 0.0 会导致方向矢量指向右侧，
这是因为 `yaw` 和 `pitch` 最后合成的矩阵是 `cameraFront`，因此我们最初向左旋转一点。

```kotlin
override fun onTouch(action: Int, x: Float, y: Float) {
    when (action) {
        MotionEvent.ACTION_DOWN -> {
            oldx = x
            oldy = y
        }
        MotionEvent.ACTION_MOVE -> {
            var xoffset = oldx - x
            var yoffset = y - oldy
            oldx = x
            oldy = y

            xoffset *= sensitivity
            yoffset *= sensitivity

            yaw += xoffset
            pitch += yoffset

            if (89f < pitch) pitch = 89.0
            if (pitch < -89f) pitch = -89.0

            val a = (cos(Math.toRadians(pitch)) * cos(Math.toRadians(yaw))).toFloat()
            val b = sin(Math.toRadians(pitch)).toFloat()
            val c = (cos(Math.toRadians(pitch)) * sin(Math.toRadians(yaw))).toFloat()

            cameraFront[0] = a
            cameraFront[1] = b
            cameraFront[2] = c

            cameraFront = normalize(cameraFront)
        }
    }
}
```

在 touch 的回调函数中，我们计算当前帧和上一帧的触摸位置偏移量。
注意 `xoffset` 和 `yoffset` 的计算，
x 轴的偏移量代表的是**偏航角**，y 同的偏移量代表的是**俯仰角**，
先讲 `xoffset`：

在操作上，当我们向右滑动时，我们希望的是摄像头向左移动（就像转铅笔一样，笔的两端的方向相反），
反之亦然。
在 OpenGL 中原点在屏幕中心，上方为 Y 轴正方向，右方为 X 轴正方向，
摄像头向左移动，则表示偏航角增大（逆时针运动了），但为负值（X 轴左方向为负值）；
对应到 Android 的 touch 事件中，
屏幕原点(0, 0)在左上角，下方为 Y 轴正方向，右方为 X 轴正方向，
往右滑动，则 y 值越大，因此此处要用 oldx 减去 x（oldx 比 x 小），
因为操作方向与摄像机方向刚好相反。
由此也能想象到，计算俯仰角时是什么情况了。

在操作上，当我们向下滑动时，我们希望的是摄像头向上移动（就像跷跷板一样），
反之亦然。
前面说了 OpenGL 的坐标系和 Android 的坐标系，X 轴方向一致，但 Y 轴方向相反。
因此，在计算俯仰角时，
摄像头向上移动时，我们需要一个正值的俯仰角，
因此此处要用 y 减去 oldy（y 比 oldy 大），
这与偏航角的计算刚好相反。

接下下，我们把偏移量加到 `pitch` 和 `yaw` 上。

不过我们需要给摄像机添加一些限制，这样摄像机就不会发生奇怪的移动了（也会避免一些奇怪的问题）。
对于俯仰角，要让用户不能看向高于 89 度的地方（在 90 度时视角会发生逆转，所以我们把 89 度作为极限），
同样也不允许小于 -89 度。
这样能够保证用户只能看到天空或脚下，但是不能超过这个限制。
因此代码中我们用了两个 if 来实现这个限制。

注意，我们没有给偏航角设置限制，这是因为我们不希望限制用户的水平旋转。
当然，给偏航角设置限制也容易，如果你愿意可以自己实现。

最后一步，就是通过用俯仰角和偏航角来计算，以得到真正的方向向量。
计算出来的方向向量就会包含根据鼠标移动计算出来的所有旋转了。

最后把归一化方法补上：

```kotlin
private fun normalize(arr: FloatArray): FloatArray {
    val len = sqrt(arr[0] * arr[0] + arr[1] * arr[1] + arr[2] * arr[2])

    arr[0] = arr[0] / len
    arr[1] = arr[1] / len
    arr[2] = arr[2] / len

    return arr
}
```

## 缩放(MyRenderer06)

> 由于模拟器无法多点触摸操作，因此我们将屏幕不同区域视作不同的操作区域。

作为我们摄像机系统的一个附加内容，我们还会来实现一个缩放(Zoom)接口。
在之前的教程中我们说**视频**（Field of View）或**fov**定义了我们可以看到场景中多大的范围，
当视野变小时，场景投影出来的空间就会减小，产生放大（Zoom In）了的感觉。

我们使用屏幕右区域来实现该功能：

```kotlin
private fun onZoom(
    yoffset: Float
) {
    if (fov in 1.0f..90.0f) {
        fov -= yoffset * sensitivity
    }
    if (fov <= 1.0f) {
        fov = 1.0f
    }
    if (90.0f <= fov) {
        fov = 90.0f
    }

    Matrix.perspectiveM(
        projection, 0,
        fov,
        aspect, 0.1f, 100f
    )
}
```

现在我们就实现了一个简单的摄像机系统了，他能够让我们在 3D 环境中自由移动。

你可以去自由地实验，如果遇到困难，可以对比 MyRenderer06 的源码。

接下来，MyRenderer07 将实现多点触摸，让我们能够同时进行多个操作。

MyRenderer08 将会把摄像机抽象出来，作为单独的一个类使用，
并且实现了两种多点触摸操作方案（Touch01 和 Touch02，Touch02 采用了协程来配合实现）并进行了封装。













-----------

今天得剁手，一个版本号写在了一个错误的地方导致了两三个小时的折腾加把代理重新配置了一遍的苦差事，这手一定要剁的。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。
