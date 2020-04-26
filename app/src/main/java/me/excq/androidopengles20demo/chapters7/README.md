## 变换

尽管我们现在已经知道了如何创建一个物体、颜色、加入纹理，给他们一些细节表现，但因为他们都还是静态物体，仍是不够有趣。
我们可以尝试着在每一帧改变物体的顶点并且重新配置缓冲区从而使他们移动，但这太繁了，而且会消耗很多的处理时间。
我们现在有一个更好的解决方案，使用（多个）**矩阵**（Matrix）对象可以更好的**变换**（Transform）一个物体。

矩阵是一种非常有用的数学工具，尽管听起来可能有些吓人，不过一量你理解了他们后，他们会变得非常有用。
在讨论矩阵的过程中，我们需要使用到一些数学知道。

为了深入了解变换，我们首先要在讨论矩阵之前进一步了解一下向量。

此处略过不表，详情可查看 [LearnOpenGL CN 教程](https://learnopengl-cn.github.io/01%20Getting%20started/07%20Transformations/)

## 实践

OpenGL 没有自带任何的矩阵和向量知识，所以我们必须定义自己的数学类和函数。
我希望的抽象所有的数学细节，使用已经做好的数学库。
幸运的是，有个易于使用，专门为 OpenGL 量身定做的数学库，那就是 `Matrix`。

```kotlin
private val trans = FloatArray(16)
Matrix.setIdentityM(trans, 0)
Matrix.translateM(trans, 0, 0.5f, 0.5f, 0f)
```

我们先定义了一个长度为 `16` 的 `float` 数组 `trans`，然后使用 `Matrix` 的 `setIdentity` 方法初始化了 `trans`。
setIdentityM 初始化一个矩阵数组，通用的矩阵数组长度为 16，是 4 * 4 结构。
初始化后，数组的 1*1, 2*2, 3*3, 4*4 的位置，即对角位置均被设置为 1。
接着我们把矩阵和一个位移向量传递给 `translateM` 方法来完成这个工作（然后用给定的矩阵乘以位移矩阵就能获得最后需要的矩阵）。

下一个问题是：如何把矩阵传递给着色器？
GLSL 里有一个 `mat4` 类型，他储存着一个 4 * 4 的 float 数组。
所以我们将修改顶点着色器让其接收一个 `mat4` 的 uniform 变量，然后再用矩阵 uniform 乘以位置向量即可：

```glsl
attribute vec4 vPosition;
attribute vec2 vTexCoord;

varying vec2 outTexCoord;

uniform mat4 transform;

void main() {
  gl_Position = transform * vPosition;
  outTexCoord = vec2(vTexCoord.x, 1.0 - vTexCoord.y);
}
```

> GLSL 也有 `mat2` 和 `mat3` 类型从而允许了像向量一样的混合运算。
> [前面提到的](https://learnopengl-cn.github.io/01%20Getting%20started/07%20Transformations/)所有数学运算在矩阵类型里都可以使用。

在把位置向量传给 `gl_Position` 之前，我们先添加一个 uniform，并且将其与变换矩阵相乘。
我们的箱子现在应该往右上角位移了四分之一个屏幕的距离。
当然，在此之前我们需要所变换矩阵传递给着色器：

```kotlin
val mTransformHandle = shader.getUniformLocation("transform")
GLES20.glUniformMatrix4fv(mTransformHandle, 1, false, trans, 0)
```

我们首先查询 uniform 变量的地址，然后用有 `Matrix4fv` 后缀的 `glUniform` 方法把矩阵数据发送给着色器。

- 第一个参数他是 uniform 的位置值；
- 第二个参数告诉 OpenGL 我们将要发送多少个矩阵，在我们的例子里是 1；
- 第三个参数询问我们是否希望对我们的矩阵进行转换（Transpose），也就是说交换矩阵的行和列。OpenGL 开发者通常使用一种内部矩阵布局，叫做**列主序**（Column-major Ordering）布局。`Matrix` 的默认布局就是列主序，所以并不需要转换矩阵，我们填 `false`；
- 最后一个参数是真正的矩阵数据。

我们创建了一个变换矩阵，在顶点着色器声明了一个 uniform，并把矩阵发送给了着色器，着色器会变换我们的顶点坐标，最后的结果应该是这样的：

todo 此处应有效果图

让我来做些更有意思的事情，让我们来旋转和缩放之前教程中的那个箱子。
首先把箱子逆时针旋转 90 度。
然后缩放 0.5 倍，使他变成原来的一半大。
我们先来创建变换矩阵：

```kotlin
Matrix.rotateM(trans, 0, 90f, 0f, 0f, 1f)
Matrix.scaleM(trans, 0, 0.5f, 0.5f, 0.5f)
```

首先，我们把箱子在每个轴都缩放到 0.5 倍，然后沿 Z 轴旋转 90 度。
注意有纹理的那面矩形是在 XY 平面上的，所以我们需要他绕着 Z 轴旋转。

现在我们再运行我们的程序，应该可以看到一个向右上位移了四分之一个屏幕，比原来小一倍且旋转了 90 度的箱子了。

现在让我们看看，如何让箱子随着时间旋转。
要让箱子随着时间旋转，我们必须在 `onDrawFrame` 循环中更新变换矩阵，因为他在每一次渲染迭代中都要更新。

```kotlin
override fun onDrawFrame(gl: GL10?) {
    ...

    Matrix.setIdentityM(trans, 0)
    Matrix.translateM(trans, 0, 0.5f, 0.5f, 0f)
    Matrix.rotateM(trans, 0, angle++, 0f, 0f, 1f)
    Matrix.scaleM(trans, 0, 0.5f, 0.5f, 0.5f)
    GLES20.glUniformMatrix4fv(mTransformHandle, 1, false, trans, 0)

    ...
}
```

要记得的是前面例子中我们可以在任何地方初始化变换矩阵，但现在我们必须在每一次迭代中初始化他，从而保证我们能够不断更新旋转角度。
这意味着我们不得不在每次循环的迭代中重新初始化变换矩阵。
通常在渲染场景的时候，我们也会有多个需要在每次渲染迭代中都用新值重新创建的变换矩阵。

在这里我们先把箱子缩放 0.5 倍，再围绕 Z 轴旋转了 angle 个角度，最后把旋转过后的箱子位移到屏幕的右上角。
记住，实际的变换顺序应该与阅读顺序相反：
尽管在代码中我们先位移后再旋转最后缩放，但实际的变换却是先应用缩放再旋转最后位移的。
明白所有这些变换的组合，并且知道他们是如何应用到物体上是一件非常困难的事情。
只有不断的尝试和实验这些变换你才能情事地掌握他们。

祝你成功。

todo 明天再见，晚安

