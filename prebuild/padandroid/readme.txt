
只需要将Android.mk中以下部分添加到polaris-common.mk中即可
#add the follow define in polaris-common.mk
PRODUCT_PACKAGES += \
	com.padandroid.aplus \
	com.padandroid.launcher.fit \
	com.padandroid.theme.Sky \
	com.padandroid.theme.tea

PRODUCT_COPY_FILES += \
	device/softwinner/polaris-common/prebuild/padandroid/default_workspace.xml:system/etc/default_workspace.xml