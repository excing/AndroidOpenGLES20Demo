## 文本渲染

> 参考：[文本渲染](https://learnopengl-cn.github.io/06%20In%20Practice/02%20Text%20Rendering/)
> 参考：[OpenType字体与TrueType字体的区别](https://www.cnblogs.com/lovelp/p/b1e0ecc5ac8f2852f7eb7fabbc95627f.html)
> 参考：[freetype-gl](https://github.com/rougier/freetype-gl)
> 参考：[Build android shared lib using ndk-build without have jni folder](https://stackoverflow.com/questions/25868989/build-android-shared-lib-using-ndk-build-without-have-jni-folder/26024451)
> 参考：[Android 官方教程: Android.mk](https://developer.android.com/ndk/guides/android_mk)
> 参考：[Android使用freetype](https://juejin.im/post/5b24dd55f265da597c771f17)

当我们在图形计算领域学习到一定阶段后，你可能会想使用 OpenGL 来绘制文本。
然而，可能与你想象的并不一样，使用像 OpenGL 这样的底层库来把文本渲染到屏幕上并不是一件简单的事情。
如果你只需要绘制 128 种不同的字符，那么事情可能会简单一些。
但是如果你要绘制的字符有着不同的宽、高和边距，事情马上就复杂了。
根据你使用的语言的不同，你可能会需要多于 128 个字符。
再者，如果你要绘制音乐符、数学符号这些特殊的符号；或者渲染竖排文本呢？
一旦你把文本这些复杂的情况考虑进来，你就不会奇怪为什么 OpenGL 这样的底层 API 没有包含文本处理了。

由于 OpenGL 本身并没有包含任何的文本处理能力，我们必须自己定义一套全新的系统让 OpenGL 绘制文本到屏幕上。
由于文本字符没有图元，我们必须要有点创造力才行。
需要使用的一些技术可以是：通过 GL_LINES 来绘制字形，创建文本的 3D 风格，或在 3D 环境中将字符纹理渲染到 2D 四边形上。

开发者最常用的一种方式是将字符纹理绘制到四边形上。
绘制这些纹理四边形本身其实并不是很复杂，然而检索要绘制文本的纹理却变成一项有挑战性的工作。
本节将探索多种文本渲染的实现方法，并且使用 FreeType 库实现一个更加高级但更灵活的渲染文本技术。

### 经典文本渲染：位图字体

早期的时候，渲染文本是通过选择一个需要的字体（Font），或者自己创建一个，并提取这个字体中所有相关的字符，
将他们放到一个单独的大纹理中来实现的。
这样的纹理叫做**位图字体**(Bitmap Font)，他在纹理的预定义区域中包含了我们想要使用的所有字符。
这些字符被称为**字形**（Glyph）。
每个字形都关联着一个特定的纹理坐标区域。
当你想要渲染一个字符的时候，你只需要通过渲染这一块特定的位图字体区域到 2D 四边形上即可。

![](https://learnopengl-cn.github.io/img/06/02/bitmapfont.png)

你可以看到，我们取一张位图字体，从纹理中采样（通过仔细选择纹理坐标）对应的字形，
并渲染他们到多个 2D 四边形上，最终渲染出“OpenGL"文本。
通过启用混合，让背景保持透明，最终就能渲染一个字符串到屏幕上。
这个位图字体是通过 Codehead的**位图字体生成器**生成的。

使用这种方式绘制文本有很多优点也有很多缺点。
产生，他相对来说很容易实现，并且因为位图字体已经预光栅化了，他的效率也很高。
然而，这种方式不够灵活。
当你想要使用不同的字体时，你需要重新编译一套全新的位图字体，而且你的程序会被限制在一个固定的分辨率。
如果你对这些文本进行缩放的话，你会看到文本的像素边缘。
此外这种方式通常会局限于非常小的字符集，如何你想让他来支持 Extended 或者 Unicode 字符集的话就很不现实了。

这种绘制文本的方式曾经得益于他的调整和可移植性而非常流行，然而现在已经出现更加灵活的方式了。
其中一个是我们即将讨论的“使用 FreeType 库来加载  TrueType 字体”的方式。

### 现代文本渲染：FreeType

FreeType 是一个能够用于加载字体并将他们渲染到位图以及提供多种字体相关的操作的软件开发库。
他是一个非常受欢迎的跨平台字体库，他被用于 Mac OS X、Java、PlayStation 该机、Linux、Android 等平台。
FreeType 的真正吸引力在于他能够加载 TrueType 字体。

FreeType 字体不是用像素或其他不可缩放的方式来定义的，他是通过数学公式（曲线的组合）来定义的。
类似于矢量图像，这些光栅化后的字体图像可以根据需要的字体高度来生成。
通过使用 TrueType 字体，你可以轻易渲染不同大小的字形而不造成任何质量损失。

> 几种轮廓字体：
> TrueType采用几何学中二次B样条曲线及直线来描述字体的外形轮廓，
> 其特点是：TrueType既可以作打印字体，又可以用作屏幕显示；
> 由于它是由指令对字形进行描述，因此它与分辨率无关，输出时总是按照打印机的分辨率输出。
> 无论放大或缩小，字符总是光滑的，不会有锯齿出现。
> 但相对PostScript字体来说，其质量要差一些。特别是在文字太小时，就表现得不是很清楚。

> OpenType也叫Type 2字体，是由Microsoft和Adobe公司开发的另外一种字体格式。
> 它也是一种轮廓字体，比TrueType更为强大，
> 最明显的一个好处就是可以在把PostScript字体嵌入到TrueType的软件中。
> 并且还支持多个平台，支持很大的字符集，还有版权保护。
> 可以说它是Type 1和TrueType的超集。
> OpenType的主要优点:
> 1）增强的跨平台功能
> 2）更好的支持Unicode标准定义的国际字符集
> 3）支持高级印刷控制能力
> 4）生成的文件尺寸更小
> 5）支持在字符集中加入数字签名，保证文件的集成功能。

FreeType 可以在他们的[官方网站](https://www.freetype.org/)中下载到。
你可以选择自己用源码编译这个库，如果支持你的平台的话，你也可以使用他们预编译好的库。
在这里，Android 平台要使用 FreeType 需要自己编译。

#### 安装及配置 NDK 和 CMake

要为我们的应用编译和调试原生代码（C/C++），需要以下组件：

- Android 原生开发套件（NDK）：可以利用这套工具在 Android 应用中使用 c 和 c++ 代码。
- CMake：一款外部编译工具，可与 Gradle 搭配使用来编译原生库。如果只计划使用 ndk-build，则不需要此组件。
- LLDB：Android Studio 用于调试原生代码的调试程序。默认情况下，LLDB 将与 Android Studio 一起安装。

**安装 NDK 和 CMake**

当我们安装 NDK 时，Android Studio 会选择最新的 NDK。
对于大多数项目，安装此默认版本的 NDK 已经足够。
如果你的项目需要一个或多个特定版本的 NDK，你可以下载并配置特定版本，
详情可参与[Android 官方教程](https://developer.android.com/studio/projects/install-ndk#specific-version)。
这样做有助于确保在每个项目都依赖于特定版本的 NDK 的情况下，项目之间的编译可重现。
Android Studio 会将所有版本的 NDK 安装到 **android-sdk/ndk/** 目录中。

要在 Android Studio 中安装 CMake 和默认的 NDK，请执行以下操作：

1. 打开项目后，依次点击 **Tools -> SDK Manager**。
2. 点击 **SDK Tools** 标签页。
3. 选中 **NDK(Side by side)** 和 **CMake** 复选框。
4. 点击 **OK**。此时系统会显示一个对话框，告诉你以上软件包将占用多少磁盘空间。
5. 点击 **OK**。
6. 安装完成后，点击 **Finish**。
7. 你的项目会自动同步编译文件并执行编译。修正发生的所有错误。

> 注意：如果你在 **ndk-bundle** 文件夹中安装了 NDK，他会显示在标签为 NDK 的列表中。
> 如果你使用的是 Android Gradle 插件 3.5.0 或更高版本，则可以选中或取消选中该复选框。
> 取消选中该复选框会卸载 NDK，释放磁盘空间，并使该复选框从列表中消失。
> 如果你卸载旧版 NDK，请从项目的 **local.properties** 文件中移除 **ndk.dir 值，该值现已弃用。

**在项目里配置 NDK**

当我们安装好 **NDK** 和 **CMake** 后，需要我们在项目中设置**NDK** 的软件包目录。
此设置的操作为：

1. 打开 **File** 菜单，选择 **Project Structure** 选项。
2. 点击 **SDK Location** 标签页。
3. 填写 **Android NDK location**选项，注意一定要具体到版本号。

#### 配置 FreeType

Android Studio 支持向 Android 项目中添加 C 和 C++ 代码，只需要将相应的代码添加到项目模块的 **cpp** 目录中即可。
在我们构建项目时，这些代码会编译到一个可由 Gradle 与 APK 打包在一起的原生库中。
然后，Java 或 Kotlin 代码即可通过 Java 原生接口（JNI）调用原生库中的函数。
如需要详细了解如何使用 JNI 框架，请参阅 [Android JNI 提示](https://developer.android.com/training/articles/perf-jni)。

Android  Studio 支持适用于跨平台项目的 CMake，以及速度比 CMake 更快但仅支持 Android 的 ndk-build。
目录不支持在同一模块中同时使用 CMake 和 ndk-build。

了解到这一点，我们就可以先下载 FreeType [最新版本](https://sourceforge.net/projects/freetype/files/freetype2/2.10.2/) 到本地了，
然后解压到 **cpp** 目录，此目录位于项目根目录下的 **app/src/main** 文件夹下，如果没有，则新建一个文件夹即可。

然后我们采用 ndk-build 方式加载和编译 FreeType，这时需要我们编写一个文件：Android.mk。
解压出来的文件夹名为 `freetype-2.10.2`，进入 `app/src/main/cpp/freetype-2.10.2` 目录，新建 `Android.mk` 文本文件，内容如下：

```mk
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := freetype2-static

LOCAL_CFLAGS := -DANDROID_NDK \
                -DFT2_BUILD_LIBRARY=1

LOCAL_C_INCLUDES := ${LOCAL_PATH}/include

LOCAL_SRC_FILES := \
  src/base/ftsystem.c \
  src/base/ftinit.c \
  src/base/ftdebug.c \
  ...[CLIST]...

include $(BUILD_STATIC_LIBRARY)
```

在这里， `LOCAL_PATH` 变量表示源文件在开发树中的位置。
其中值 `my-dir` 是构建系统提供的内置宏函数，表示将返回当前目录（Android.mk 文件本身所在的目录）的路径。

下一行声明的 `CLEAR_VARS` 变量，其值由构建系统提供。
他指向一个特殊的 GNU Makefile，后者会为你清除许多 `LOCAL_XXX` 变量，
例如 `LOCAL_MODULE`、`LOCAL_SRC_FILES`、`LOCAL_STATIC_LIBRARIES` 等。
注意，GNU Makefile 不会清除 `LOCAL_PATH`。
此变量必须保留其值，因为系统在单一 GNU Make 执行上下文（其中的所有变量都是全局变量）中解析所有构建控制文件。
在描述每个模块之前，必须声明（重新声明）此变量。

第三行的 `LOCAL_MODULE` 变量存储我们要构建的模块名称（即 Java 代码中 `System.loadLibrary` 方法的参数）。
请在应用的每个模块中使用一次此变量。

每个模块名称必须唯一，且不含任何空格。

`LOCAL_CFLAGS` 表示编译时的参数。

下面的 `LOCAL_C_INCLUDES` 变量是用来指定相对于 NDK `root` 目录的路径列表，
以便在编译所有源文件时添加到 include 搜索路径中。
在这里，就是 FreeType 库的原生代码，
FreeType 中 `include` 目录里的 `ft2build.h` 头文件是其入口函数，因此只需要导入这个目录即可。

`LOCAL_SRC_FILES` 表示参与编译的源文件，上面示例没有全部写完，完整版本可见 [freetype-2.10.2/Android.mk]()，
该源文件列表来自 [freetype-2.10.2/docs/INSTALL.ANY]() 说明文件。

最后一行 `include $(BUILD_STATIC_LIBRARY)` 帮助构建系统将一切连接到一起，最终编译成静态库 `.a` 文件。
`BUILD_STATIC_LIBRARY` 变量指向一个 GNU Makefile 脚本，
该脚本会收集我们自最近 `include` 以来在 `LOCAL_XXX` 变量中定义的所有信息。
但构建系统不会将静态库复制到您的项目/软件包中，但可以使用静态库构建共享库。

然后我还要在 `app/src/main/cpp` 目录里新建一个 Android.mk，以引导 ndk-build 找到其他的 `Android.mk`，内容如下：

```mk
include $(all-subdir-makefiles)
```

表示导入所有子目录的 ndk-build 的配置文件。

接下来，我们在 `build.gradle` 里加入 `native NDK` 的配置：

```gradle
android {
    ...
    defaultConfig { ... }

    externalNativeBuild {
        ndkBuild {
            path "src/main/cpp/Android.mk"
        }
    }

    ...
}

dependencies { ... }
```

其中 `path` 指定 Android.mk 构建脚本的相对路径。

OK，到这里，就把 FreeType 成功导入到我们的项目中来了，不过我们还没有使用 FreeType 来做任何操作。
所以也无法看到任何信息。

下面，我们新建一个 `jni01.c` 来测试一下。
在 `app/src/main/cpp` 目录下新建 `chpaters11` 文件夹，新建 `jni01.c`，内容如下：

```c
#include <string.h>
#include <jni.h>
#include <ft2build.h>
#include FT_FREETYPE_H

JNIEXPORT jstring JNICALL
Java_me_excq_androidopengles20demo_chapters11_MainActivity_stringFromJNI( JNIEnv* env, jobject thiz )
{
   FT_Library ft;
   if (FT_Init_FreeType(&ft))
       return (*env)->NewStringUTF(env, "ERROR::FREETYPE: Could not init FreeType Library !.");

   FT_Face face;
   if (FT_New_Face(ft, "fonts/arial.ttf", 0, &face))
       return (*env)->NewStringUTF(env, "ERROR::FREETYPE: Failed to load font !");
   else
       return (*env)->NewStringUTF(env, "FREETYPE: Successed to load font !");
}
```

这是一个 JNI 函数，由 Java 层的 me.excq.androidopengles20demo.chapter11.MainActivity 类的 `stringFromJNI` 方法接收。
其中 `FT_Init_FreeType` 和 `FT_New_Face` 函数是 FreeType 的 API，他们会返回一个错误信息（如果有），
我们根据这个错误信息，返回一串字符。Java/kotlin 层代码如下：

```kotlin
package me.excq.androidopengles20demo.chapters11

import ...

class MainActivity : BaseActivity() {
    ...
    override fun onCreate(savedInstanceState: Bundle?) {
        ...

        println("onCreate ${stringFromJNI()}")
    }

    external fun stringFromJNI(): String?

    companion object {
        init {
            System.loadLibrary("freetype")
        }
    }
}
```

到这里还不行，我们还要一个 `Android.mk`。
在 `app/src/main/cpp/chapters11` 目录里新建一个 `Android.mk`，内容如下：

```mk
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := freetype

LOCAL_C_INCLUDES := ${LOCAL_PATH}/../freetype-2.10.2/include

LOCAL_SRC_FILES := jni01.c

LOCAL_STATIC_LIBRARIES := freetype2-static

include $(BUILD_SHARED_LIBRARY)
```

这个跟 `app/src/main/cpp/freetype-2.10.2/Android.mk` 的配置差不多，多了几个变量，如下：

- LOCAL_STATIC_LIBRARIES: 用于存储当前模块依赖的静态库模块列表，在这里我们需要 `freetype2-static` 静态库。
- BUILD_SHARED_LIBRARY: 指向的构建脚本会收集您在 LOCAL_XXX 变量中提供的模块的所有相关信息，以及确定如何根据您列出的源文件构建目标可执行文件。

重新运行，即可在 Logcat 标签页里看到我们打印的日志了：

```log
onCreate ERROR::FREETYPE: Failed to load font !
```

没错，就是这行，因为我们没有 `fonts/arial.ttf` 这个字体文件。
我们可以使用 Android Studio 里的 Build 菜单的 Analyze APK 选项，打开我们的 Debug 包，
检查一下我们刚刚运行的 APK 文件结构，我们会发现 `lib` 文件夹里有一个 `libfreetype.so` 文件，
这个就是上面 `cpp/chapters11/Android.mk` 里 `LOCAL_MODULE` 里配置的共享库名称。

> 今天问了教主，才能把 FreeType 成功导入到项目中，不然你们看不到上面的教程了。
> 用 ndk-build 看着也挺简单的哦，不过里面有很多的知识点，我不知道，搞的非常狼狈，比如 Android.mk 配置文件，
> 一个库要配置一个 Android.mk，我本来想当然全局只用了一个 Android.mk，当然的编译失败了，
> 然后我这个 Android.mk 文件里，FreeType 需要编译的源文件我没有填写，也就是说 `LOCAL_SRC_FILES` 只有一个 `jni01.c`，
> 结果编译时理所当然说找不到 `FT_Init_FreeType` 和 `FT_New_Face` 函数了。
> 嗯，大概就是 ndk-build 没用熟导致的。

另外，使用命令行 ndk-build 也可以把我们的 c/c++ 源文件编译为 `.so` 文件，使用以下命令即可：

```cmd
%ANDROID_NDK%/ndk-build APP_BUILD_SCRIPT=path/to/path/to/project/Android.mk NDK_PROJECT_PATH=path/to/path/to/project
```

注意，在这里，`Android.mk` 配置里的 `LOCAL_SRC_FILES` 要直接写**相对 `APP_BUILD_SCRIPT` 的路径**，否则无法通过编译。
编译成功后，我们会在 `APP_BUILD_SCRIPT` 目录时看到生成的 `.so` 文件。

好了，导入 FreeType 库用了昨、今两天时间，不能浪费时间了，我们继续文本渲染。

### 使用 FreeType 渲染文本

FreeType 所做的事就是加载 TrueType 字体并为每一个字形生成位图以及计算几个度量值（Metric）。
我们可以提取出他生成的位图作为字形的纹理，并使用这些度量值定位字符的字形。

要加载一个字体，我们只需要初始化 FreeType 库，并且将这个字体加载为一个 FreeType 称之为**面**（face）的东西。
这里为我们加载一个从**Windows/Fonts**目录中拷贝来的 TrueType 字体文件 **arial.ttf**。

FreeType 在 Android 平台上默认加载的 unicode 字符，需要使用 GetStringChars 方法获取，
为了这个方法，我花了一天多时间去找，真是。。无知真可怕。

todo 虽然文本渲染还是没有成功，不过还是有一点成果的，汉字显示可用了。今天就这样了，明天继续，加油。