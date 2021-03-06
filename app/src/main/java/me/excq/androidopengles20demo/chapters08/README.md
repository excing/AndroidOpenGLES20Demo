## 坐标系统

在 [chapters7](https://github.com/excing/AndroidOpenGLES20Demo/tree/master/app/src/main/java/me/excq/androidopengles20demo/chapters7) 中，我们学习了如何有效地利用矩阵的变换来对所有顶点进行变换。
OpenGL 希望在每次顶点着色器运行后，我们可见的所有顶点都为标准化设备坐标（Normailzed Device Coordinate, NDC）。
也就是说，每个顶点的 `x`, `y`, `z`, 坐标都应该在 `-1.0` 到 `1.0` 之间，超出这个坐标范围的顶点都将不可见。
我们通常会自己设定一个坐标的范围，之后再在顶点着色器中将这些会想起谁燃气锅炉 标准化设备坐标。
然后将这些标准化设备坐标传入光栅器（Rasterizer），将他们变换为屏幕上的二维坐标或像素。

将坐标变换为标准化设备坐标，接着再转化为屏幕坐标的过程通常是分步进行的，也就是类似于流水线那样子。
在流水线中，物体的顶点在最终转化为屏幕坐标之前还会被谜团到多个坐标系统（Coordinate System）。
将物体的坐标变换到几个**过渡**坐标系（Intermediate Coordinate System）的优点在于，
在这些特定的坐标系统中，一些操作或运算更加方便和容易，这一点很快就会变得很明显。
对我们来说比较重要的总共有 5 个不同的坐标系统：

- 局部空间（Local Space，或者称为物体空间（Object Space））
- 世界空间（World Space）
- 观察空间（View Space，或者称为视觉空间（Eye Space））
- 裁剪空间（Clip Space）
- 屏幕空间（Screen Space）

这就是一个顶点在最终被转化为片段之前需要经历的所有不同状态。

你现在可能会对什么是坐标被单，什么是坐标系统感到非常困惑，所以我们将用一种更加通俗的方式来解释他们。
下面，我们将显示一个整体的图片，之后我们讲解每个空间的具体功能。

## 概述

为了将坐标从一个坐标系变换到另一个坐标系，我们需要乃至几个变换矩阵，
最重要的几个分别是**模型**（Model）、**观察**（View）、**投影**（Projection）三个矩阵。
我们的顶点坐标起始于局部空间，在这里他称为局部坐标（Local Coordinate），
他在之后会变为世界坐标（World Coordinate），观察坐标（View Coordinate），裁剪坐标（Clip Coordinate），
并最后以屏幕坐标（Screen Coordinate）的形式结束。
整个流程大致如下：

todo 此处应有流程图

1. 局部坐标是对象相对于局部原点的坐标，也是物体起始的坐标。
2. 下一步是局部坐标变换为世界空间坐标，世界空间坐标是牌一个更大的空间范围的。这些坐标相对于世界的全局原点，他们会和其他物体一起相对于世界的原点进行摆放。
3. 接下来我们将世界坐标变换为观察空间坐标，使得每个坐标老师从摄像机或者说观察者的角度进行观察的。
4. 坐标到达观察空间之后，我们需要将其投影到裁剪坐标。裁剪坐标会被处理到 `-1.0` 到 `1.0` 的范围内，并判断哪些顶点将会出现在屏幕上。
5. 最后，我们将裁剪坐标变换为屏幕坐标，我们将使用一个叫做**视口变换**（Viewport Transform）的过程。礼品变换将位于 `-1.0` 到 `1.0` 范围的坐标变换到由 `glViewport`· 方法所定义的坐标范围内。最后变换出来的坐标将会送到光栅器，将其转化为片段。

你可能已经大致了解了每个坐标空间的作用。
我们之所以将顶点变换到各个不同的空间的原因是有些操作在特定的坐标系统中才有意义且更方便。
例如，当需要对物体进行修改的时候，在局部空间中来操作会更说得通；
如果要对一个物体做出一个相对于其他我们位置的操作时，在世界坐标系统中来做这个才更说得通等等。
**如果我们愿意，我们也可以定义一个直接从局部空间变换到裁剪空间的变换矩阵，但那样会后会失去很灵活性。**

## 局部空间

局部空间是指物体所在的坐标空间，即对象最开始所在的地方。
想象你在一个建模软件中创建了一个立方体。
你创建的立方体的原点有可能位于（0, 0, 0），即便他有可能最后在程序中处于完全不同的位置。
甚至有可能你创建的所有模型都以 （0, 0, 0） 为初始位置。
所以，你的模型的所有顶点都是在局部空间中：他们相对于你的物体来说都是局部的。

我们一起使用的那个箱子的顶点都被设定在  `-0.5` 到 `0.5` 的坐标范围中，(0, 0) 是他的原点，这些都是局部坐标。

## 世界空间

如果我们将所有的物体导入到程序当中，他们有可能会全挤在世界的原点上，这并不是我们想要的结果。
我们想为每一个物体定义一个位置，从而能在更大的世界当中放置他们。
世界空间中的坐标正如其名：是指顶点相对于世界的坐标。
如果你希望将物体分散在世界上摆放（特别是非常真实的那样），这就是你希望物体变换到的空间。
物体的坐标将会从局部变换到世界空间；该变换是由**模型**矩阵（Model Matrix）实现的。

模型矩阵是一种变换矩阵，他能通过对物体进行位移、缩放、旋转来将他置于他本应该在的位置或朝向。
你可以将他想你为变换一个房子，你需要先将他缩小（他在局部空间中太大了），并将其位移至交警的一个小镇，
然后在 Y 轴上往左旋转一点以搭配附近的房子。
你也可以把上一节将箱子到处摆放的场景中用的那个矩阵大致看到一个模型矩阵，将箱子的局部坐标变换到场景/世界中的不同位置。

## 观察空间

观察空间经常被人们称之 OpenGL 的**摄像机**（Camera）（所以有时也称为**摄像机空间**（Camera Space）或**视觉空间**（Eye Space））。
观察空间是一将世界空间坐标转化为用户视野前方的坐标而产生的结果。
因此观察空间就是从摄像机的视角所观察到的空间。
而这通常是由一系列的位移和旋转的组合来完成，平移/旋转场景从而使得特定的对象被变换到摄像机的前方。
这些组合在一起的变换通常存储在一个**观察矩阵**（View Matrix）里，他被用来将世界坐标变换到观察空间。

## 裁剪空间

在一个顶点着色器运行的最后，OpenGL 期望所有的坐标都能落在一个特定的范围内，且任何在这个范围之外的点都应该被裁剪掉（Clipped）。
被裁剪掉的坐标被会被忽略，所以剩下的坐标就将变为屏幕上可见的片段。
这也就是**裁剪空间**名字的贴出来。

因为将所有可见的坐标都指定在 `-1.0` 到 `1.0` 的范围内不是很直观，所以我们会指定自己的坐标集（Coordinate Set），
并将他变换回标准化设备坐标系，就像 OpenGL 期望的那样。

为了将顶点坐标从观察变换到裁剪空间，我们需要定义一个**投影矩阵**（Projection Matrix），他指定了一个范围的坐标，
比如在每个维度上的 `-1000` 到 `1000`。
投影矩阵接着会将在这个指定的范围内鬼坐标变换为标准化坐标的范围（-1.0, 1.0）。
所有在范围外的坐标不会被映射到 `-1.0` 到 `1.0` 的范围之间，所以会被裁掉。

> 如果只是图元（Primitive），例如三角形，其一部分超出了**裁剪体积**（Clipping Volume），则 OpenGL 会重新构建这个三角形为一个或多个三角开让其能够适合这个裁剪范围。

由投影矩阵创建的**观察箱**（Viewing Box）被称为**平截头体**（Frustum），每个出现在平截头体范围内的坐标都会最终出现在用户的屏幕上。
将特定范围内的坐标转化到标准化设备坐标系的过程，被称之为**投影**（Projection），
因为使用投影矩阵能将 3D 坐标投影（Project）到很容易映射到 2D 的标准化设备坐标系中。

一旦所有顶点被变换到裁剪空间，最终的操作——**透视除法**（Perspective Division）将会执行，
在这个过程中我们将位置向量的 `x`, `y`, `z` 分量分别除以向量的齐次 `w` 分量；
透视除法是将 4D 裁剪空间坐标变换为 3D 标准化设备坐标的过程。
这一步会在每个顶点着色器运行的最后被自动执行。

在这一阶段之后，最终的坐标将会被映射到屏幕空间中（使用 `glViewport` 中的设定），并被变换成片段。

将观察坐标变换为裁剪坐标的投影辞职可以为两种不同的形式，每种形式都定义了不同的平截头体。
我们可以选择创建一个**正射投影矩阵**（Orthographic Projection Matrix）或一个**透视投影矩阵**（Perspective Projection Matrix）。

### 正射投影

参见 [Learn OpenGL CN 坐标系统的正射投影](https://learnopengl-cn.github.io/01%20Getting%20started/08%20Coordinate%20Systems/#_6)一节

### 透视投影

参见 [Learn OpenGL CN 坐标系统的透视投影](https://learnopengl-cn.github.io/01%20Getting%20started/08%20Coordinate%20Systems/#_6) 一节

## 抒他们都组合到一起

我们为上述的每一个步骤都创建了一个变换矩阵：模型矩阵、观察矩阵和投影矩阵。
一个顶点坐标将会根据以下过程被变换到裁剪坐标：

```math
V(clip) = M(projection) * M(view) * M(model) * V(local)
```

注意矩阵运算的顺序是相反的（记住我们需要从右往左阅读矩阵的乘法）。
最后的顶点应该被赋值到顶点着色器的 `gl_Position`，OpenGL 将会自动进行透视除法和裁剪。

> 然后呢？
> 顶点着色器的输出要求所有的顶点都在裁剪空间内，这正是我们刚才使用变换矩阵所做的。
> OpenGL 然后对裁剪坐标执行透视除法从而将他们变换到标准化设备坐标。
> OpenGL 会使用 `glViewPort` 内部的参数来将标准化设备坐标映射到屏幕坐标，每个坐标都关联了一个屏幕上的点。
> 这个过程称为视口变换。

到这里为至，可能会比较不好理解，如果你仍然不确定每个空间的作用的话（第一次看的时候我也是），也不必太担心。
接下来你会看到我们是怎样运用这些坐标空间的，而且之后会也有足够多的例子。

## 进入 3D

既然我们知道了如何将 3D 坐标变换为 2D 坐标，我们可以开始使用真正的 3D 物体，而不是枯燥的 2D 平面了。

在开始进行3D 绘图时，我们首先创建一个模型矩阵。
这个模型矩阵包含了位移、缩放与旋转操作，他们会被应用到所有物体的顶点上，以变换他们到全局的世界空间。
让我们变换一下我们的平面，将其绕着 X 轴旋转，使他看起来像放在地上一样。
这个模型矩阵看起来是这样的：

```kotlin
private val model = FloatArray(16)
Matrix.setIdentityM(model, 0)
Matrix.rotateM(model, 0, -55f, 1.0f, 0.0f, 0.0f)
```

通过将顶点坐标乘以这个模型矩阵，我们将该顶点坐标变换到世界坐标。
我们的平面看起来就是在地板上，代表全局世界里的平面。

接下来我们需要创建一个观察矩阵。
我们想要在场景里面稍微往后移动，以使得物体变成可见的（当在世界空间时，我们位于原点 `(0, 0, 0)`）。
要想在场景里面移动，先仔细想一想下面的句子：

- 将摄像机向后移动，和将整个场景向前移动是一样的。

这正是观察矩阵所做的，我们以相反于摄像机移动的方向移动整个场景。
因为我们想要往后移动，并且 OpenGL 是一个右手坐标系（Right-handed System），所以我们需要沿着 Z 轴的正方向移动。
我们会通过将场景沿着 Z 轴负方向平衡来实现。
他会给我们一种我们在往后移动的感觉。

在下一个教程中，我们将会详细讨论如何在场景中移动。
就目前来说，观察矩阵是这样的：

```kotlin
private val view = FloatArray(16)
Matrix.setIdentityM(view, 0)
Matrix.translateM(view, 0, 0f, 0f, -3f)
```

最后我们需要做的是定义一个投影矩阵。
我们希望在场景中使用透视投影，所以像这样场景一个投影矩阵：

```kotlin
private val projection = FloatArray(16)
Matrix.setIdentityM(projection, 0)
Matrix.perspectiveM(projection, 0, 45f, width.toFloat() / height, 0.1f, 100f)
```

既然我们已经创建了变换矩阵，我们应该将他们传入着色器。
首先，让我们在顶点着色器中声明一个 uniform 变换矩阵，然后将他乘以顶点坐标：

```glsl
attribute vec4 vPosition;
attribute vec2 vTexCoord;

varying vec2 outTexCoord;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
  gl_Position = projection * view * model * vPosition;
  outTexCoord = vec2(vTexCoord.x, 1.0 - vTexCoord.y);
}
```

我们还应该将矩阵传入着色器（这通常在每次渲染迭代中进行，因为变换矩阵会经常变动）：

```kotlin
val mModelHandle = shader.getUniformLocation("model")
val mViewHandle = shader.getUniformLocation("view")
val mProjectionHandle = shader.getUniformLocation("projection")
GLES20.glUniformMatrix4fv(mModelHandle, 1, false, model, 0)
GLES20.glUniformMatrix4fv(mViewHandle, 1, false, view, 0)
GLES20.glUniformMatrix4fv(mProjectionHandle, 1, false, projection, 0)
```

我们的顶点坐标已经使用模型、观察和投影辞职进行变换了，最终的物体应该会：

- 稍微向后倾斜至地板方向
- 离我们有一些距离
- 有透视效果（顶点越远，变得越小）。

我们看一下效果图：

todo 此处应有效果图

他看起来就像是一个 3D 的平面，静止在一个虚构的地板上。
如果你得到的不是相同的结果，可参考 MyRenderer01。

## 更多的 3D

到目前为止，我们一起都在使用一个 2D 平面。所以让我们大胆的拓展我们的 2D 平面为一个 3D 立方体。
要想渲染个立方体，我们一共需要 36 个顶点（6个面*每个面有 2 个三角形 * 每个三角形有 3 个顶点），
这 36 个顶点的位置如下（从第 5 个顶点开始）：

```kotlin
private val vertex = floatArrayOf(
    // 坐标            // 纹理坐标
    0.5f, 0.5f, 0.0f, 1.0f, 1.0f,
    0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
    -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
    -0.5f, 0.5f, 0.0f, 0.0f, 1.0f,

    // 以下为立方体顶点
    -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
    0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
    0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
    0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
    -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
    -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,

    -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
    0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
    0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
    0.5f, 0.5f, 0.5f, 1.0f, 1.0f,
    -0.5f, 0.5f, 0.5f, 0.0f, 1.0f,
    -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,

    -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
    -0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
    -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
    -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
    -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
    -0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

    0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
    0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
    0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
    0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
    0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
    0.5f, 0.5f, 0.5f, 1.0f, 0.0f,

    -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,
    0.5f, -0.5f, -0.5f, 1.0f, 1.0f,
    0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
    0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
    -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
    -0.5f, -0.5f, -0.5f, 0.0f, 1.0f,

    -0.5f, 0.5f, -0.5f, 0.0f, 1.0f,
    0.5f, 0.5f, -0.5f, 1.0f, 1.0f,
    0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
    0.5f, 0.5f, 0.5f, 1.0f, 0.0f,
    -0.5f, 0.5f, 0.5f, 0.0f, 0.0f,
    -0.5f, 0.5f, -0.5f, 0.0f, 1.0f
)
```

为了有趣一点，我们让立方体随着时间旋转：

```kotlin
Matrix.rotateM(model, 0, angle++, 0.5f, 1.0f, 0.1f)
```

然后我们使用 `GLES20.glDrawArrays` 来绘制立方体，但这次共有 36 个顶点：

```kotlin
GLES20.glDrawArrays(
    GLES20.GL_TRIANGLES,
    4, // 从第四个顶点数据开始，因为前面 4 个顶点数据是 MyRenderer01 的顶点数据，此处未删除，如果删除了，应填 0
    36
)
```

如果一切顺利的话，你应该能得到下面这样的效果（源代码见 MyRenderer02）：

todo 此处应有效果图（视频）

这的确有点像是一个立方体，但又有种说不出的奇怪。
立方体的某些本就被遮挡住的面被绘制在了这个立方体其他面之上。
之所以这样是因为 OpenGL 是一个三角形一个三角形地来绘制你的立方体的，
所以即使之前那里有东西他也会覆盖之前的像素。
因为这个原因，有些三角形会被绘制在其他三角形上面，虽然他们本不应该是被覆盖的。

幸运的是，OpenGL 存储尝试信息在一个叫做**Z缓冲**（Z-buffer）的缓冲中，他允许 OpenGL 决定何时覆盖一个像素而何时不覆盖。
通过使用 Z 缓冲，我们可以配置 OpenGL 来进行尝试测试。

### Z 缓冲

OpenGL 存储他的所有尝试信息于一个 Z 缓冲中，也被称为**深度缓冲**（Depth Buffer）。
GLFW 会自动为你生成这样一个缓冲（就像他也有个颜色缓冲来存储输出图像的颜色）。
深度值存储在每个片段里面（作为片段的 **z** 值），
当片段想要输出他的颜色时，OpenGL 会将他的尝试值与 Z 缓冲进行比较，
如果当前的片段在其他片段之后，他将会被丢弃，否则将会覆盖。
这个过程称为**深度测试**（Depth Testing），他是由 OpenGL 自动完成的。

然而，如果我们想要确定 OpenGL 真的执行了深度测试，首先我们要告诉 OpenGL 我们想要启用深度测试。
他默认是关闭的，我们可以通过 `glEnable` 方法来开户深度测试。

`glEnable` 和 `glDisable` 方法允许我们启用或禁用某个 OpenGL 功能。
这个功能会一起保持启用/禁用状态，直到另一个调用来禁用/启用他。
现在我们想启用深度测试，需要开启 `GL_DEPTH_TEST`:

```kotlin
GLES20.glEnable(GLES20.GL_DEPTH_TEST)
```

因为我们使用了深度测试，我们也想要在每次渲染迭代之前清除深度缓冲（否则前一帧的深度信息仍然保存在缓冲中）。
就像清除颜色缓冲一样，我们也可心通过大 `glClear` 函数中指定 `DEPTH_BUFFER_BIT` 位来清除尝试缓冲：

```kotlin
GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
```

我们来重新运行下程序看看 OpenGL 是否执行了深度测试：

todo 此处应有效果图（视频）

就这样，一个开启了没试测试，各个面都是纹理，并且还在旋转的立方体。
如果你的程序有什么问题，请参考 MyRenderer03 对比一下。

### 更多的立方体

现在我们想在屏幕上显示 10 个立方体。
每个立方体看起来都是一样的，区别在于他们在世界的位置及旋转角度不同。
立方体的图形布局已经定义好了，所以当渲染更多物体的时候我们不需要改变我们的缓冲数组和属性数组，
我们唯一需要做的只是改变每个对象的模型矩阵来将立方体变换到世界坐标系中。

首先，让我们为每个立方体定义一个位移向量来指定他在世界空间的位置。
我们将在一个 `float` 数组中定义 10 个立方体位置：

```kotlin
private val cubePositions = floatArrayOf(
    0.0f, 0.0f, 0.0f,
    2.0f, 5.0f, -15.0f,
    -1.5f, -2.2f, -2.5f,
    -3.8f, -2.0f, -12.3f,
    2.4f, -0.4f, -3.5f,
    -1.7f, 3.0f, -7.5f,
    1.3f, -2.0f, -2.5f,
    1.5f, 2.0f, -2.5f,
    1.5f, 0.2f, -1.5f,
    -1.3f, 1.0f, -1.5f
)
```

10 个立方体的位置，每 3 个 float 数值为一个立方体的 x, y, z 的坐标相对位置。

现在在 `onDrawFrame` 循环中，我们调用 `glDrawArrays` 10次，但这次在我们渲染之前每次传入一个不同的模型矩阵到顶点着色器中。
我们将会在游戏循环中创建一个小的循环用不同的模型矩阵渲染我们的物体 10 次。
注意我们也对每个箱子加了一点旋转：

```kotlin
GLES20.glUniformMatrix4fv(mViewHandle, 1, false, view, 0)
GLES20.glUniformMatrix4fv(mProjectionHandle, 1, false, projection, 0)

for (i in 0..9) {
    angle += 0.1f
    Matrix.setIdentityM(model, 0)
    Matrix.translateM(
        model,
        0,
        cubePositions[i * 3 + 0],
        cubePositions[i * 3 + 1],
        cubePositions[i * 3 + 2]
    )
    Matrix.rotateM(
        model,
        0,
        20f * i + angle,
        1.0f,
        0.3f,
        0.5f
    )

    GLES20.glUniformMatrix4fv(mModelHandle, 1, false, model, 0)

    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
    GLES20.glDrawArrays(
        GLES20.GL_TRIANGLES,
        0,
        36
    )
}
```

这段代码将会在每次新门诊本绘制出来的时候更新模型矩阵，如此总共重复 10 次。
然后我们应该就能看到一个拥有 10 个正在奇葩地旋转着的立方体世界。

todo 此处应有效果图（视频）

## 总结

我们在同一个 GLSurfaceView 中（其实是在 同一个 GLThread 线程中），同时操作多个 OpenGL Program 时，
他们之间是会相互影响的，比如我们先进入 MyRenderer01 再进入 MyRenderer04 后再回来，
出现的情况就是：

先看到一个斜斜的 2D 图片，然后看到 10 个旋转着的立方体，最后回到 MyRenderer01 时会发现 2D 图片消失了，界面一片空白。
MyRenderer02 也有一样的情况。

这是因为，
当有其他地方开启了深度测试，那么此处（MyRenderer01 和 MyRenderer02）如果不禁用深度测试，那么，便默认启用了深度测试。
并且（前提是其他地方开启了深度测试，比如先进入 MyRenderer04 再回来），
因为没有清除深度缓冲，前一帧的深度信息仍然保存在缓冲中，导致的现象就是空白，MyRenderer01 也有同样的情况。
如果在将 GLES20.glClear 的参数设置为 GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT，
便会发现，其（MyRenderer02）效果与  MyRenderer03 一样。

想要避免这种情况，可以在 glClear 方法之前，添加 glDisable(GLES20.GL_DEPTH_TEST)，
便禁用了深度测试。

