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

现在我们胡了 `deltaTime`，在计算速度的时候可以将其考虑进去了：

```kotlin
override fun onTouch(action: Int, x: Float, y: Float) {
    val cameraSpeed = 0.005f * deltaTime
    ...
}
```

与前面的部分结合在一起，我们有了一个更流畅，且在不同硬件上拥有相同速度的摄像机系统：

todo 此处应有效果图（视频）

如果你卡住了，查看一下 MyRenderer04 的源码。

todo 今天就到这里了，明天再见，晚安