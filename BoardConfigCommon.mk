# BoardConfig.mk
#
# Product-specific compile-time definitions.
#

# cpu stuff
TARGET_CPU_ABI := armeabi-v7a
TARGET_CPU_ABI2 := armeabi
TARGET_CPU_SMP := true
TARGET_CPU_VARIANT := cortex-a7
TARGET_ARCH := arm
TARGET_ARCH_VARIANT := armv7-a-neon
ARCH_ARM_HAVE_TLS_REGISTER := true


TARGET_USE_NEON_OPTIMIZATION := true
TARGET_ARCH_LOWMEM := false
TARGET_BOARD_PLATFORM := polaris
TARGET_BOOTLOADER_BOARD_NAME := exdroid
CEDARX_USE_ION_MEM_ALLOCATOR:=Y

USE_OPENGL_RENDERER := true

# use our own init.rc
TARGET_PROVIDES_INIT_RC :=true

# no hardware camera
USE_CAMERA_STUB := true
# Set /system/bin/sh to ash, not mksh, to make sure we can switch back.
# TARGET_SHELL := ash

# audio & camera & cedarx
CEDARX_CHIP_VERSION := F50
CEDARX_USE_SWAUDIO := Y

#widevine
BOARD_WIDEVINE_OEMCRYPTO_LEVEL := 3

# image related
TARGET_NO_BOOTLOADER := true
TARGET_NO_RECOVERY := true
TARGET_NO_KERNEL := false

INSTALLED_KERNEL_TARGET := kernel
BOARD_KERNEL_BASE := 0x40000000
#BOARD_KERNEL_CMDLINE := console=ttyS0,115200 rw init=/init loglevel=4
TARGET_USERIMAGES_USE_EXT4 := true
BOARD_FLASH_BLOCK_SIZE := 4096
BOARD_SYSTEMIMAGE_PARTITION_SIZE := 805306368
#TARGET_USERIMAGES_SPARSE_EXT_DISABLED := true
#BOARD_USERDATAIMAGE_PARTITION_SIZE := 1073741824

# hardware module include file path
TARGET_HARDWARE_INCLUDE := $(TOP)/device/softwinner/polaris-common/hardware/include

BOARD_SEPOLICY_DIRS := \
        device/softwinner/polaris-common/sepolicy

BOARD_SEPOLICY_UNION := \
        app.te \
        device.te \
        file.te \
        file_contexts \
        genfs_contexts \
        healthd.te \
        installd.te \
        mount.te \
        netd.te \
        surfaceflinger.te \
        system.te \
        untrusted_app.te \
        vold.te \
        zygote.te


BOARD_CHARGER_ENABLE_SUSPEND := true
