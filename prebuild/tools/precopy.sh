#!/system/bin/busybox sh

BUSYBOX="/system/bin/busybox"

if [ ! -e /data/system.notfirstrun.precopy ]; then
    $BUSYBOX cp -fp /system/precopy/*.apk /data/app/
    $BUSYBOX touch /data/system.notfirstrun.precopy
fi

