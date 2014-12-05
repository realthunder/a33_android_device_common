#!/system/bin/busybox sh

echo "do lights_leds job"
	if [ -e /sys/class/leds/red_led ];then
		chmod 664 /sys/class/leds/red_led/brightness
		chown root:system /sys/class/leds/red_led/brightness
		chmod 664 /sys/class/leds/red_led/trigger
		chown root:system /sys/class/leds/red_led/trigger
		if [ -e /sys/class/leds/red_led/delay_on ];then
			chmod 664 /sys/class/leds/red_led/delay_on
			chown root:system /sys/class/leds/red_led/delay_on		
			chmod 664 /sys/class/leds/red_led/delay_off
			chown root:system /sys/class/leds/red_led/delay_off
		fi
	fi
	if [ -e /sys/class/leds/green_led ];then
		chmod 664 /sys/class/leds/green_led/brightness
		chown root:system /sys/class/leds/green_led/brightness
		chmod 664 /sys/class/leds/green_led/trigger
		chown root:system /sys/class/leds/green_led/trigger
		if [ -e /sys/class/leds/green_led/delay_on ];then
			chmod 664 /sys/class/leds/green_led/delay_on
			chown root:system /sys/class/leds/green_led/delay_on		
			chmod 664 /sys/class/leds/green_led/delay_off
			chown root:system /sys/class/leds/green_led/delay_off
		fi		
	fi
	if [ -e /sys/class/leds/blue_led ];then
		chmod 664 /sys/class/leds/blue_led/brightness
		chown root:system /sys/class/leds/blue_led/brightness
		chmod 664 /sys/class/leds/blue_led/trigger
		chown root:system /sys/class/leds/blue_led/trigger
		if [ -e /sys/class/leds/blue_led/delay_on ];then
			chmod 664 /sys/class/leds/blue_led/delay_on
			chown root:system /sys/class/leds/blue_led/delay_on		
			chmod 664 /sys/class/leds/blue_led/delay_off
			chown root:system /sys/class/leds/blue_led/delay_off
		fi		
	fi