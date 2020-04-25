## 6、纹理

我们已经了解到，我们可以为每个顶点添加颜色来增加图形的细节，从而创建出有趣的图像。
但是，如果想让图形看起来更真实，我们就必须有足够多的顶点，从而指定足够多的颜色。
这将会产生很多额外开销，因为每个模型都会需要更多的顶点，每个顶点又需要一个颜色属性。

艺术家和程序员更喜欢使用纹理（Texture）。
纹理是一个 2D 图片（甚至也有 1D 和 3D 的纹理），他可以用来添加物体的细节；
你可以想你纹理是一张绘有砖块的纸，无缝折叠贴合到你的 3D 的房子上，这样你的房子看起来就像有砖墙外表了。
因为我们可以在一张图片上插入非常多的细节，这样就可以让物体非常精细而不用指定额外的顶点。

> 除了图像以外，纹理也可以被用来储存大量的数据，这些数据可以发送到着色器上，但这不是我们现在的主题。

为了能够把纹理映射（Map）到三角形上，我们需要指定三角形的每个顶点各自对应纹理的哪个部分。
这样每个顶点就会关联着一个纹理坐标（Texture Coordinate），用来标明该从纹理图像的哪个部分采样（译注：采集片段着色）。
之后在图形的其他片段上进行片段插值（Fragment Interpolation）。

纹理坐标在 x 和 y 轴上，范围到 0 到 1 之间（注意我们使用的是 2D 纹理图像）。
使用纹理坐标获取纹理颜色叫做**采样**（Sampling）。
纹理坐标起始于（0, 0），也就是纹理图片的左下角，终始于（1, 1），即纹理图片的右上角。

todo 此处应有示例图

我们为三角形指定了 3 个纹理坐标点。
如上图所示，我们希望三角形的左下角对应纹理的左下角，因此我们把三角形左下角顶点的纹理坐标设置为（0, 0）；
三角形的上顶点对应于图片的上中位置，所以我们把他的纹理坐标设置为（0.5, 1.0）；
同理右下方的顶点设置为（1, 0）。
我们只要给顶点着色器传递这三个纹理坐标就行了，接下来他们会被传到片段着色器中，他会为每个片段进行纹理坐标的插值。

纹理坐标看起来就像这样：

```kotlin
private val texCoords = floatArrayOf(
    0.0f, 0.0f,
    1.0f, 0.0f,
    0.5f, 1.0f
)
```

对纹理采样的解释非常宽松，他可以采用几种不同的插值方式。
所以我们需要自己告诉 OpenGL 该怎么样对纹理采样。

### 纹理环绕方式

纹理坐标的范围通常是从（0, 0）到（1, 1），那如果我们把纹理坐标设置在范围之外会发生什么？
OpenGL 默认的行为是重复这个纹理图像（我们基本上忽略浮点纹理坐标的整数部分），但 OpenGL 提供了更多的选择：

环绕方式（Wrapping） | 描述
-------------------| ------
GL_REPEAT          | 对纹理的默认行为。重复纹理图像。
GL_MIRRORED_REPEAT | 和 GL_REPEAT 一样，但每次重复图片是镜像放置的。
GL_CLAMP_TO_EDGE   | 纹理坐标会被约束在 0 到 1 之间，超出的部分会重复纹理坐标的边缘，产生种边缘被拉伸的效果。
GL_CLAMP_TO_BORDER | 超出的坐标为用户指定的边缘颜色。

当纹理坐标超出默认范围时，每个选项都有不同的视觉效果输出。
我们来看看这些纹理图像的例子：

todo 此处应有示例图

前面提到的每个选项都可以使用 `glTexParameter` 函数对单独的一个坐标轴设置
（即 `str`，他们和 `xyz` 等价，如果没有使用 3D 纹理那么就没有 `r`）：

```kotlin
GLES20.glTexParameteri(
    GLES20.GL_TEXTURE_2D,
    GLES20.GL_TEXTURE_WRAP_S,
    GLES20.GL_MIRRORED_REPEAT
)
GLES20.glTexParameteri(
    GLES20.GL_TEXTURE_2D,
    GLES20.GL_TEXTURE_WRAP_T,
    GLES20.GL_MIRRORED_REPEAT
)
```

第一个参数指定了纹理目标，我们使用的是 2D 纹理，因此纹理目标是 `GL_TEXTURE_2D`。
第二个参数需要我们指定设置的选项与应用的纹理轴，我们打算配置的是 `WRAP` 选项，并且指定 `S` 和 `T` 轴。
最后一个参数需要我们传递一个环绕方式，在这个例子中 OpeGL 会给当前激活的纹理设定纹理环绕方式为 `GL_MIRRORED_REPEAT`。

如果我们选择 `GL_CLAMP_TO_BORDER` 选择，我们还需要指定一个边缘的颜色。
这需要使用 `glTexParameter` 函数的 `fv` 后缀形式，用 `GL_TEXTURE_BORDER_COLOR` 作为他的选项，并且传递一个 float 数组作为边缘的颜色值：

```kotlin
val borderColor = floatArrayOf(
    1.0f, 1.0f, 0.0f, 1.0f
)
GLES20.glTexParameterfv(
    GLES20.GL_TEXTURE_2D,
    GLES32.GL_TEXTURE_BORDER_COLOR,
    borderColor,
    0
)
```

> GLES20 类中没有 `GL_TEXTURE_BORDER_COLOR`

### 纹理过滤

纹理坐标不依赖于分辩率（Resolution），他可以是任意浮点值，
所以 OpenGL 需要知道怎样将纹理像素映射到纹理坐标。
当你有一个很大的物体但是纹理的分辨率很低的时候这就变得很重要了。
你可以已经猜到了，OpeGL 也有对于纹理过滤（Texture Filtering）的选择。
纹理过滤有很多个选项，但在现在我们只讨论最重要的两种：`GL_NEAREST` 和 `GL_LINEAR`

> Texture Pixel 也叫 Texel，你可以想象你打开一张 `.jpg` 格式图片，不断放大你会发现他是由无数像素点组成的，这个点就是纹理像素。
>注意不要和纹理坐标搞混，纹理坐标是你给模型顶点设置的那个数组，
>OpenGL 以这个顶点的纹理坐标数据去查找纹理图像上的像素，然后进行采样提取纹理像素的颜色。

`GL_NEAREST` 也叫**邻近过滤**（Nearest Nighbor Filtering），是 OpenGL 默认的纹理过滤方式。
当设置为 `GL_NEAREST` 的真是过分上，OpenGL 会选择中心点最接近纹理坐标的那个像素。下图中你可以看到四个像素，加号代表纹理坐标。
左上角那个纹理像素的中心距离纹理坐标最近，所以他会被选择为样本颜色：

todo 此处应有图片

`GL_LINEAR` 也叫线性过滤，(Bi)linear Filtering，他会基于纹理坐标附近的纹理像素，计算出一个插值，
近似出这些纹理像素之间的颜色。
一个纹理像素的中心距离纹理坐标越近，那么这个纹理像素的颜色对最终的样本颜色的贡献越大。
下图中你可以看到返回的颜色是邻近像素的混合色：

todo 此处应有图片

那么这两种纹理过滤方式有怎样的视频效果呢？让我产看看在一个很大的牺上应用一张低分辨率的纹理会发生什么吧：
（纹理被放大了，每个纹理像素都能看到）

todo 此处应有图片

`GL_NEAREST` 产生了颗粒状的图案，我们能够清晰看到组成纹理的像素，
而 `GL_LINEAR` 能够产生更平滑的图案，很难看出单个的纹理像素。
`GL_LINEAR` 可以产生更真实的输出，但有些开发都更喜欢 `8-bit` 风格，所以他们会用 `GL_NEAREST` 选项。

当进行**放大**（Magnify） 和**缩小**（Minify）操作的时候可以设置纹理过滤的选项，
比如你可以在纹理被缩小的时候使用邻近过滤，被放大时使用线性过滤。
这段代码看起来会和纹理环绕方式的设置很相似：

```kotlin
GLES20.glTexParameteri(
    GLES20.GL_TEXTURE_2D,
    GLES20.GL_TEXTURE_MIN_FILTER,
    GLES20.GL_NEAREST
)
GLES20.glTexParameteri(
    GLES20.GL_TEXTURE_2D,
    GLES20.GL_TEXTURE_MAG_FILTER,
    GLES20.GL_LINEAR
)
```

### 多级渐远纹理

想象一下，假设我们有一个包含着上千物体的大房间，每个物体上都有纹理。
有些物体会很远，但其纹理拥有与近处物体同样高的分辨率。
由于远处的物体可能只产生很少的片段，OpenGL 从高分辨率纹理中为这些片段获取正确的颜色值就很困难，
因为他需要对一个跨过纹理很大部分的片段只拾取一个纹理颜色。
在小物体上这会产生不真实的感觉，更不用说对他们使用高分辨率纹理浪费内存的问题了。

OpenGL 使用一种叫做**多级渐远纹理**（Mipmap）的概念来解决这个问题。
他简单来说就是一系列的纹理图像，后一个纹理图是前一个的二分之一。
多级渐远纹理背后的理念很简单：距观察者的距离超过一定的阈值，OpenGL 会使用不同的多级渐远纹理，即最适合物体的距离的那个。
由于距离远，解析度不高也不会被用户注意到。
同时，多级渐远纹理另一加分之处是他的性能非常好。
让我们看一下多级渐远纹理是什么样子的：

todo 此处应有图片

手工为每个纹理图像创建一系列多级渐远纹理很麻烦，
幸好 OpenGL 有一个 `glGenerateMipmaps` 方法，在创建完一个纹理后调用他，OpenGL 就会承担接下来的所有工作了。

在渲染中切换多级渐远纹理级别（Level）时，OpenGL 在两个不同级别的多级渐远纹理层之间会产生不真实的生硬边界。
就像普通的纹理过滤一样，切换多级渐远纹理级别时，你也可以在两个不同多级渐远纹理级别之间使用 `NEAREST` 和 `LINEAR` 过滤。
为了指定不同多级渐远纹理级别之间的过滤方式，你可以使用下面四个选项中的一个代替原有的过滤方式：

过滤方式 | 描述
--------| ---
`GL_NEAREST_MIPMAP_NEAREST` | 使用最邻近的多级渐远纹理来匹配像素大小，并使用邻近插值进行纹理采样
`GL_LINEAR_MIPMAP_NEAREST` | 使用最邻近的多级渐远纹理级别，并使用线性插值进行采样
`GL_NEAREST_MIPMAP_LINEAR` | 在两个最匹配像素大小的多级渐远纹理之间进行线性插值，使用邻近插值进行采样
`GL_LINEAR_MIPMAP_LINEAR`  | 在两个邻近的多级渐远纹理之间使用线性插值，并使用线性插值进行采样

就像纹理过滤一样，我们可以使用 `glTexParmateri` 将过滤方式设置为前面四种提到的方法之一：

```kotlin
GLES20.glTexParameteri(
    GLES20.GL_TEXTURE_2D,
    GLES20.GL_TEXTURE_MIN_FILTER,
    GLES20.GL_LINEAR_MIPMAP_LINEAR
)
GLES20.glTexParameteri(
    GLES20.GL_TEXTURE_2D,
    GLES20.GL_TEXTURE_MAG_FILTER,
    GLES20.GL_LINEAR
)
```

一个常见的错误是，将放大过滤的选项设置为多级渐远纹理过滤选项之一。
这样没有任何效果，因为多级渐远纹理主要是使用在纹理被缩小的情况下的：
纹理放大不会使用多级渐远纹理，为放大过滤设置多级渐远纹理的选项会产生一个 `GL_INVALID_MNUM` 的错误代码。

## 加载与创建纹理

使用纹理之前要做的第一件事是把他们加载到我们的应用中。

```kotlin
val input: InputStream?
val bitmap: Bitmap?
val options = BitmapFactory.Options()
try {
    options.inJustDecodeBounds = false
    options.inPreferredConfig = Bitmap.Config.ARGB_8888

    input = assets.open("chapters6/container.jpg")
    bitmap = BitmapFactory.decodeStream(input, null, options)
} catch (e: Exception) {
    e.printStackTrace()
    throw e
}

val width = options.outWidth
val height = options.outHeight
val image = ByteBuffer.allocateDirect(width * height * 4)
    .order(ByteOrder.nativeOrder())
bitmap?.copyPixelsToBuffer(image)
image.position(0)
```

以上代码会得到储存图片的字节数组，以及图片的宽高，这在后面我们生成纹理的时候会用到。

## 生成纹理

和之前生成的 OpenGL 对象一样，纹理也是使用 ID 引用 的。
让我们来创建一个：

```kotlin
textures = IntBuffer.allocate(1)
GLES20.glGenTextures(1, textures)
```

`glGenTextreus` 方法首先需要输入生成纹理的数量，然后把他们储存在第二个参数的 `IntBuffer` 数组中，
就像其他对象一样，我们需要绑定他，让之后任何纹理的指令都可以配置当前绑定的纹理：

```kotlin
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
```

现在纹理已经绑定了，我们可以使用前面载入的图片数据生成一个纹理了。纹理可以通过 `glTexImage2D` 方法来生成：

```kotlin
GLES20.glTexImage2D(
    GLES20.GL_TEXTURE_2D,
    0,
    GLES20.GL_RGBA,
    width,
    height,
    0,
    GLES20.GL_RGBA,
    GLES20.GL_UNSIGNED_BYTE,
    image
)
GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
```

函数很大，参数也不少，所以我们一个个地讲：

- 第一个参数指定了纹理目标（Target）。设置为 `GL_TEXTURE_2D` 意味着会生成与当前绑定的纹理对象在同一个目标上的纹理（任何绑定到 `GL_TEXTURE_1D` 和 `GL_TEXTURE_3D` 的纹理不会受到影响）
- 第二个参数为纹理指定多级渐远纹理的级另，如果你希望单独手动设置每个多级渐远纹理的级别的话。这里我们填0，也就是基本级别。
- 第三个参数告诉 OpenGL 我们希望把纹理储存为何种格式。我们的图像时是采用 `ARGB_8888` 格式获取时，因此我们也把纹理储存为 `RGBA`。
- 第四个和第五个参数设置最终的纹理宽度和高度。
- 第六个参数总是被设为 0（历史遗留问题）。
- 第七第八个参数定义了源图的格式的数据类型，我们的图像时是采用 `ARGB_8888` 格式获取时，因此我们也把纹理储存为 `RGBA`。
- 最后一个参数是真正的图像数据。

当调用 `glTexImage2D` 时，当前绑定的纹理对象就会被附加上纹理图像。
然而，目前只有基本级别（Base-level）的图像被加载了，如果要使用多级渐远纹理，我们必须手动设置所有不同的图像（不断递增第二个参数）。
或者，直接在生成纹理之后调用 `glGenerateMipmap`。
这会为当前绑定的强龙不压地头蛇自动生成所有需要的多级渐远纹理。

生成了纹理和相应的多级渐远纹理后，释放图像的内存并解绑纹理对象是一个很好的习惯。

```kotlin
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
input.use { i -> i.close() }
bitmap?.recycle()
```

> 注意：当使用 `GLES20.glTexImage2D` 方法生成纹理时，
> 其中 `image` 值必须是通过 `Bitmap.Config.ARGB_8888` 获取的，
> 即 `options.inPreferredConfig = Bitmap.Config.ARGB_8888`，
> 不过 `BitmapFactory.Options()` 默认就是 `ARGB_8888`。
> 通过 `ARGB_8888` 获取的 Bitmap 大小，每个像素是 4 个字节，
> 因此 `ByteBuffer` 的大小是 `width * height * 4`。

这里推荐使用 `GLUtils` 类的 `texImage2D` 方法生成纹理，这个方法只需要接受一个 `Bitmap` 对象即可，
并对 `Bitmap` 没有颜色通道要求，可以是 `ARGB_8888`，也可以是 `RGB_565`。具体如下：

```kotlin
val input: InputStream?
val bitmap: Bitmap?
try {
    input = assets.open("chapters6/container.jpg")
    bitmap = BitmapFactory.decodeStream(input)
} catch (e: Exception) {
    e.printStackTrace()
    throw e
}

textures = IntBuffer.allocate(1)

GLES20.glGenTextures(1, textures)
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])

GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
input.use { i -> i.close() }
bitmap?.recycle()
```

所以生成一个纹理的过程看起来应该是这样的：

```kotlin
val Textures = IntBuffer.allocate(1)
GLES20.glGenTextures(1, textures)
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0])
// 为当前绑定的纹理对象设置环绕、过滤方式

...

// 加载并生成纹理
val input: InputStream?
val bitmap: Bitmap?
try {
    input = assets.open("chapters6/container.jpg")
    bitmap = BitmapFactory.decodeStream(input)
} catch (e: Exception) {
    e.printStackTrace()
    throw e
}

GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
input.use { i -> i.close() }
bitmap?.recycle()
```

## 应用纹理

后面的这部分我们会使用 `glDrawElements` 绘制一个矩形。
我们需要告知 OpenGL 如何采样纹理，所以我们必须使用纹理坐标更新顶点数据：

```kotlin
private val vertex = floatArrayOf(
    // 坐标           // 纹理坐标
    0.5f, 0.5f, 0.0f, 1.0f, 1.0f,
    0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
    -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
    -0.5f, 0.5f, 0.0f, 0.0f, 1.0f
)
```

由于我们去掉了原有的颜色信息，添加了新的纹理坐标信息，所以我们必须告诉 OpenGL 新的顶点格式：

```kotlin
GLES20.glVertexAttribPointer(
    mTextureHandle,
    2,
    GLES20.GL_FLOAT,
    false,
    20,
    12
)
```

这里我们的两个顶点属性的步长参数是：`5 * 4 = 20` 个字节，一个 `float` 是四字节长度，一个顶点属性有 5 个 float 信息。
同时，纹理坐标是从第 4 个 float 开始的，因此偏移量是 `3 * 4 = 20` 个字节。

接着我们需要调整顶点着色器使其能够接受纹理坐标，并把纹理坐标传给片段着色器：

```glsl
attribute vec4 vPosition;
attribute vec2 vTexCoord;

varying vec2 outTexCoord;

void main() {
  gl_Position = vPosition;
  outTexCoord = vTexCoord;
}
```

片段着色器应该把输出变量 `outTexCoord` 作为输入变量。

片段着色器也应该能访问纹理对象，但我们怎样能把纹理对象传给片段着色器呢？
GLSL 有一个供纹理对象使用的内建数据类型，叫做**采样器**（Sampler），他对纹理类型作为后缀，
比如 `sampler1D`、`sampler3D`，还有在我们的例子中的 `sampler2D`。
我们可以简单声明一个 `uniform sampler2D` 把一个纹理添加到片段着色器中，稍后我们会把纹理赋值给这个 `uniform`。

```glsl
precision mediump float;

varying vec2 outTexCoord;

uniform sampler2D ourTexture;

void main() {
  gl_FragColor = texture2D(ourTexture, outTexCoord);
}
```

我们使用 GLSL 内建的 `texture2D` 函数来采样纹理的颜色，他第一个参数是纹理采样器，第二个参数是对应的纹理坐标。
`texture2D` 函数会使用之前设置的纹理参数对相应的颜色值进行采样。
这个片段着色器的输出就是纹理的（插值）纹理坐标上的（过滤后的）颜色。

> 注：`texture2D` 是 OpenGL ES 2.0 的函数，ES 3.0 中被 `texture` 函数替代了。

现在只剩下在调用 `glDrawElements` 之前绑定纹理了，他会自动把纹理赋值给片段着色器的采样器：

```kotlin
GLES20.glEnableVertexAttribArray(mPositionHandle)
GLES20.glEnableVertexAttribArray(mTextureHandle)

GLES20.glVertexAttribPointer(
    mPositionHandle,
    3,
    GLES20.GL_FLOAT,
    false,
    20,
    0
)

GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures!![0])
GLES20.glUniform1i(mOurTextureHandle, 0)
GLES20.glVertexAttribPointer(
    mTextureHandle,
    2,
    GLES20.GL_FLOAT,
    false,
    20,
    12
)

GLES20.glDrawElements(
    GLES20.GL_TRIANGLES,
    6,
    GLES20.GL_UNSIGNED_SHORT,
    0
)

GLES20.glDisableVertexAttribArray(mPositionHandle)
GLES20.glDisableVertexAttribArray(mTextureHandle)
```

todo 此处应有效果图

如果你的矩形是全黑或全白的，你可以在哪儿做错了什么。
比如颜色管道参数配置错了，或者绘制时没有绑定纹理，也可能绘制后又解绑纹理了，等等。
梁君诺对比一下[源码]()。
todo 缺少源码链接

todo 明天再见
