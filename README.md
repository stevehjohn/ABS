# ABS - Android Battery Saver

I have terrible mobile reception at home, so my phone uses WiFi for calls and SMSs most of the time.

However the mobile radio is still searching hard for a signal and this hammers the battery.

By turning **on** Airplane mode and then re-enabling WiFi (and Bluetooth if required) this forces use of WiFi calling and disables the mobile radio. 

I found this extended my phone's battery life from barely a day, to _1 week_!

## So, what does this app do?

This app will allow you to specify certain WiFi access points where you always want to use WiFi calling.

When it detects you are connected to one, it will perform the Airplane mode, re-enable WiFi/Bluetooth procedure automatically. When you disconnect, it reverts the phone to normal usage.

## What's with all the permission requests?

Unfortunately, to get WiFi network names Android requires location permissions. As this service monitors in the background, it also needs the permission even when the app UI isn't running. Lastly, Android forces you to show a notification for a contiunally running service.

# TODOs

- Pop-up explaining why permissions are required if a user denies them.