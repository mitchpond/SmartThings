# SmartThings
ST DeviceTypes and SmartApps

Device Tamper Alarm:
A simple Smart App that will watch a number of devices and send a notification when they are tampered with. Has preliminary support for activating alarms, but I do not have an alarm to test with...
Currently only looks for contact sensors, since there is no standard way to report tampering. Could easily be expanded to other devices if the community or SmartThings decides on a standard event/attribute for this.

Quirky Tripper:
Comfortable with making this a 1.0 release at this point.

1.0:
Added tile to indicate that the tamper alarm has been tripped. Tile must be reset manually by tapping it in the ST app.
Removed Refresh capability, since there's really nothing to refresh.
Updated to request an hourly battery report. For me, this worked for two hours, then stopped entirely. Your mileage may vary.
I do get an initial battery report 9 times out of 10 on a fresh "join".
If your device never reports battery, remove the device, factory reset it (hold the tamper switch while removing and replacing the battery) and re-add to ST.
