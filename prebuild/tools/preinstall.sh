#!/system/bin/busybox sh

BUSYBOX="/system/bin/busybox"

/system/bin/chown system:system /dev/block/by-name/misc
/system/bin/chmod 0600 /dev/block/by-name/misc

/system/bin/chown rild:media /dev/mux*
/system/bin/chmod 0777 /dev/mux*

mkdir /bootloader
mount -t vfat /dev/block/by-name/bootloader /bootloader

if [ ! -e /data/system.notfirstrun ]; then
    echo "do preinstall job"

    /system/bin/sh /system/bin/pm preinstall /system/preinstall
    /system/bin/sh /system/bin/pm preinstall /sdcard/preinstall
    /system/bin/sh /system/bin/data_copy.sh


    $BUSYBOX touch /data/system.notfirstrun

    echo "preinstall ok"
else
    echo "do nothing"
fi

umount /bootloader
rmdir /bootloader
