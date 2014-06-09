# Bluetooth Proximity Lock
- - -

This app runs a service in the background which monitors the approximate physical distance of bluetooth peripherals from the user's device. Should the bluetooth peripheral go out of range, the user's device lockscreen will be enabled. When in range, the device will have its lockscreen disabled for easy access.

This app will be published on Google Play for a low price when it reaches a more complete state, but it will contain no license-checking. It will also remain open-source (under the Apache license) and freely-available for those who wish to compile it for themselves; all I ask is that users please not distribute compiled packages.

Planned features:

- [x] Lock device when bluetooth peripheral leaves user-defined radius (approximate).
- [ ] Keep device unlocked when it is connected to a user-specified "trusted" WiFi network.
- [ ] Save battery by only running the service right as the device screen is woken up.
- [ ] If the above is disabled, user can enable immediate locking when BT device leaves defined range.
