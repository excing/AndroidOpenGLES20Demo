## 画笔填充

采用扫描线种子填充算法. 实现步骤如下：

先获取一个种子（坐标点，可以是用户点击的坐标点或随机的一个坐标点或某封闭区域内的一个坐标点等等），
然后向该种子的 -x 和 x 方向延伸，直到遇到与种子颜色不同的坐标点停止，两个方向分别记录为 xLeft 和 xRight。
记录这两点坐标以备填充使用，并将该两点作为新的一对种子，这对种子即第一条扫描线的起点和终点。
然后该扫描线分别向上和向下扫描，如果起点种子在轮廓线内，则向 x 方向扫描，反之向 -x 方向扫描，
同理，终点种子如果在轮廓线内，则向 -x 方向扫描，反之向 x 方向扫描。

扫描线种子填充算法大概逻辑就是这样。

### 获取下一点的颜色

种子填充需要知道下一点的颜色，在这里我们使用 OpenGLES 的 `glReadPixels` 函数，
`glReadPixels` 函数调用参数如下：

```java
public static native void glReadPixels(
    int x,
    int y,
    int width,
    int height,
    int format,
    int type,
    java.nio.Buffer pixels
);
```

其中：

- x: 屏幕的 x 轴坐标；
- y: 屏幕的 y 轴坐标，这里需要注意的是，y 轴的原点是画布（屏幕）的左下角，而 Android 的坐标原点是左上角。
- width, height: 获取区域的宽高；
- format: 指定像素信息的格式，格式有：`GL_RGB`、`GL_RGBA`，分别返回像素的红绿蓝、红绿蓝alpha，如果只想要红色信息，可以填 `GL_RED`，类似的值还有 `GL_GREEN`, `GL_BLUE` 等，更多的值可以参考 <https://www.khronos.org/registry/OpenGL-Refpages/gl4/html/glReadPixels.xhtml>
- type: `pixels` 的数据类型，可以填 `GL_FLOAT`, `GL_BYTE`, `GL_UNSIGNED_BYTE` 等值；
- pixels: 像素信息缓存，GL 把读取的像素信息存储到该缓存中，如果 `type` 参数值为 `GL_FLOAT` 类型，那么 `pixels` 的类型应为 `FloatBuffer`。

**在 Java 层，`type` 参数值设为 `GL_FLOAT`，在模拟器上可以获取像素信息，但在真机上无法获取，总是返回 0，**
**`GL_BYTE` 也是同样的情况，设置为 `GL_UNSIGNED_BYTE` 后，则可以恢复正常。**
具体的情况，可以查看这个 issues: [glReadPixels 在模拟器上返回正确，但在真机上总是返回 0](https://github.com/excing/AndroidOpenGLES20Demo/issues/3)

### 获取 y 坐标

因为 `glReadPixels` 函数中 y 坐标值的原点是左下角，因此在 Android 平台中，我们需要对此值进行反转，
如下：

```kotlin
y = surfaceHeight - y
```

### 获取用户点击的像素颜色

在我们把 `type` 参数设为 `GL_UNSIGNED_BYTE` 后，`glReadPixels` 函数返回的 `pixel` 缓存，其值为无符号，
因此需要对其进行转换：

```kotlin
val r = pixels.get(0).getUByte()
val g = pixels.get(1).getUByte()
val b = pixels.get(2).getUByte()
```

当然如果不需要打印或操作该颜色，也可以直接使用无符号数值，获取用户点击的像素颜色时，我们便没有对其进行转换。

```kotlin
val color = ByteArray(3)
GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, pixel)
pixel.position(0)

color[0] = pixel.get(0)
color[1] = pixel.get(1)
color[2] = pixel.get(2)
```

保存原始像素颜色，用于后续扫描判断使用。

### 获取第一条扫描线

第一条扫描线是根据用户点击的点向两边扫描得到的。代码如下：

```kotlin
var xLeft = x
var xRight = x

// 向左扫描的第一个种子
while (isSameColor(xLeft - 1, y) && 0 < xLeft) {
    xLeft--
}

// 向右扫描的第一个种子
while (isSameColor(xRight + 1, y) && xRight < surfaceWidth) {
    xRight++
}

private fun isSameColor(x: Int, y: Int): Boolean {
    GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, pixel)
    pixel.position(0)
    return (r == pixel.get(0) &&
            g == pixel.get(1) &&
            b == pixel.get(2))
}
```

`xLeft` 和 `xRight` 即为第一线扫描线的起点和终点。

### 完整的代码

获取到第一条扫描线后，将这条扫描线分别向上和向下继续扫描，即可。

完整的代码参见 [MainActivity](https://github.com/excing/AndroidOpenGLES20Demo/blob/master/app/src/main/java/me/excq/androidopengles20demo/toy02/MainActivity.kt)

该实现仅支持对凸边形的填充，暂不支持对其他形状的闭合区域进行填充。

接下来我们将实现如何对全类型闭合区域的填充。

todo 任意形状填充，明天做吧，今天做不完了，对了，现在的实现，在轮廓边缘会出现大量的空白，也要解决，晚安。

