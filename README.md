# visor - low vision magnifier

Visor is a magnifying glass for your iPhone, iPad and iPod touch.
Enlarge difficult to read text by 4 magnification levels and change contrast
to 5 different viewing modes. Optionally, turn on the LED flash of
your device to help you see better in low-light conditions.

Visor for iOS supports AirPlay for streaming
your magnified content to your TV and VoiceOver.

During the day, there is a lot printed text to read:

- restaurant menus,
- price tags,
- the newspaper,
- letters or
- photos from your loved ones

visor zooms and improves the contrast of whatever you want to see.
Its advanced color modes and simple interface are made for giving
you the best possible perception of printed text on your screen
like you'd expect it from a handheld electronic magnifier.

Our website: http://visorapp.de/

## Android version

![Screenshot of the current android version](https://christian-illies.info/upload/visor-screenshot-v1.0-normal.png)
![Black White High Contrast Mode](https://christian-illies.info/upload/visor-screenshot-v1.0-b-w.png)
![White Black High Contrast Mode](https://christian-illies.info/upload/visor-screenshot-v1.0-w-b.png)
![Blue Yellow High Contrast Mode](https://christian-illies.info/upload/visor-screenshot-v1.0-b-y.png)
![Yellow Blue High Contrast Mode](https://christian-illies.info/upload/visor-screenshot-v1.0-y-b.png)

This is the offical android version of the visor app - the low vision magnifier.

[Now available in the Google PlayStore](https://play.google.com/store/apps/details?id=de.visorapp.visor)

## Changelog

Version 1.5.0 (2017-07-05)
- using a native module to calculate yuv2rgb (greyscaled) (increases performance a bit, I guess)
- using matrix-scaling instead of re-creating a bitmap with new size (drastically increases performance)
- added PhotoView to pinch and zoom to paused camera preview (see https://github.com/chrisbanes/PhotoView)
- added button animations
- added sound effects for focus and image storing
- hiding statusbar AND navigationbar
- fixed several issues

Version 1.3.0 (2016-04-23)

- If no filter is selected the app now avoids to create a bitmap. This increases battery life and app performance.
- The bitmap create thread now generates a s/w rgb representation because it's a bit faster and all color filter don't need a full color set.
- Settings slightly optimized for some devices. On my LG G4 the performance is much better. Increased parallel thread count for bitmap creation.
- Added a new button: Pause the camera image.
- Additionally store the paused image to your device.

Version 1.2.1 (2015-10-19)

- (User-Request) the app now does not adjust the brightness level.

Version 1.2 (2015-10-15)

- tap and hold for permanent autofocus mode
- store zoom level and filter settings for the next app start
- hide buttons that have no device support (zoom and flash button)

Version 1.1 (2015-08-20)

- restricted num of threads in background for bitmap processing to 1 to avoid performance drops

Version 1.0 (2015-08-13)

- add custom color modes: black on white with enhanced contrast,
  white on black, yellow on blue and blue on yellow.
- added background thread to handle image processing
- added original buttons from the iOS app

Version 0.3 (2015-08)

- added talk-back support
- added language support for german talk-back

Version 0.2 (2015-08-01)

- add app icon
- replaced button with rounded image buttons
- fixed some issues for devices that don't support color effects on the camera preview (i.e. Nexus 10).
- using a relative layout which fits to all kinds of devices.

Version 0.1 (2015-07-30)

- added camera live preview
- added autofocus on tap
- added button to toggle flashlight
- added button to change up to 7 color modes.
- added button to zoom through 4 magnification levels

## Licence

Copyright (c) 2015 Christian Illies

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Sources

### App Icon

![App Icon Source](https://upload.wikimedia.org/wikipedia/commons/0/0e/OpenEye_icon.svg)

https://commons.wikimedia.org/wiki/File:OpenEye_icon.svg

Author: [Mun May Tee-Galloway](http://ow.ly/QW51D)

Licence: [Creative Commons Attribution-Share Alike 3.0 Unported](https://creativecommons.org/licenses/by-sa/3.0/deed.en)
