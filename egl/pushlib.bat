adb  remount

adb push libGLESv2_mali.so  /system/lib/egl
adb push libGLESv1_CM_mali.so /system/lib/egl
adb push libEGL_mali.so /system/lib/egl
adb push libMali.so  /system/lib/

adb  shell sync
