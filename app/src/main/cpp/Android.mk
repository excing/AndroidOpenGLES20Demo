LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := freetype
LOCAL_CFLAGS := -DANDROID_NDK

LOCAL_C_INCLUDES := ${LOCAL_PATH}/freetype-2.10.2/include

LOCAL_SRC_FILES := \
 ${LOCAL_PATH}/chapters11/main.c
 ${LOCAL_PATH}/src/autofit/autofit.c
 ${LOCAL_PATH}/src/base/ftbase.c
 ${LOCAL_PATH}/src/base/ftbbox.c
 ${LOCAL_PATH}/src/base/ftbdf.c
 ${LOCAL_PATH}/src/base/ftbitmap.c
 ${LOCAL_PATH}/src/base/ftcid.c
 ${LOCAL_PATH}/src/base/ftfstype.c
 ${LOCAL_PATH}/src/base/ftgasp.c
 ${LOCAL_PATH}/src/base/ftglyph.c
 ${LOCAL_PATH}/src/base/ftgxval.c
 ${LOCAL_PATH}/src/base/ftinit.c
 ${LOCAL_PATH}/src/base/ftmm.c
 ${LOCAL_PATH}/src/base/ftotval.c
 ${LOCAL_PATH}/src/base/ftpatent.c
 ${LOCAL_PATH}/src/base/ftpfr.c
 ${LOCAL_PATH}/src/base/ftstroke.c
 ${LOCAL_PATH}/src/base/ftsynth.c
 ${LOCAL_PATH}/src/base/fttype1.c
 ${LOCAL_PATH}/src/base/ftwinfnt.c
 ${LOCAL_PATH}/src/bdf/bdf.c
 ${LOCAL_PATH}/src/bzip2/ftbzip2.c
 ${LOCAL_PATH}/src/cache/ftcache.c
 ${LOCAL_PATH}/src/cff/cff.c
 ${LOCAL_PATH}/src/cid/type1cid.c
 ${LOCAL_PATH}/src/gzip/ftgzip.c
 ${LOCAL_PATH}/src/lzw/ftlzw.c
 ${LOCAL_PATH}/src/pcf/pcf.c
 ${LOCAL_PATH}/src/pfr/pfr.c
 ${LOCAL_PATH}/src/psaux/psaux.c
 ${LOCAL_PATH}/src/pshinter/pshinter.c
 ${LOCAL_PATH}/src/psnames/psnames.c
 ${LOCAL_PATH}/src/raster/raster.c
 ${LOCAL_PATH}/src/sfnt/sfnt.c
 ${LOCAL_PATH}/src/smooth/smooth.c
 ${LOCAL_PATH}/src/truetype/truetype.c
 ${LOCAL_PATH}/src/type1/type1.c
 ${LOCAL_PATH}/src/type42/type42.c
 ${LOCAL_PATH}/src/winfonts/winfnt.c

include $(BUILD_SHARED_LIBRARY)
