[![](https://jitpack.io/v/LeonardoSM04/HarmonicColorExtractor.svg)](https://jitpack.io/#LeonardoSM04/HarmonicColorExtractor)

# Harmonic Color Extractor
This library provides a simple way to obtain 3 harmonic colors among them based on an image. This is based on the way Android Oreo's Media Notification does it:

![alt text](https://i0.wp.com/9to5google.com/wp-content/uploads/sites/4/2017/06/dp3_notification_media_theme_1.jpg?resize=1600%2C842&quality=82&strip=all&ssl=1)

## Importing the Library
Simply add the following dependency to your build.gradle file to use the latest version:

```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
```
dependencies {
  implementation 'com.github.LeonardoSM04:HarmonicColorExtractor:1.0.0'
}
```
## Usage
```
HarmonicColors harmonicColors = new HarmonicColorExtractor.Builder.setBitmap(bitmap).getColors();
```
- **HarmonicColors** is a class that contains the three colors extracted from the image. You can get them by using *.getBackgroundColor()*, *.getFirstForegroundColor()* and *.getSecondForegroundColor()*.
- You have to set a bitmap by using *.setBitmap(Bitmap bitmap)* and use *.getColors()* at the end in order to get the colors.
- You can customize the way the library gets the colors:
  - Set the background extraction region by using *.setBackgroundRegion(int left, int top, int right, int bottom)*.
  - Set the foreground extraction region by using *.setForegroundRegion(int left, int top, int right, int bottom)*.
  - You can resize the size of the bitmap by using *.setResizeBitmapArea(int resizeBitmapArea)*.
  
  For example:
  ```
  HarmonicColors harmonicColors = new HarmonicColorExtractor.Builder.setBitmap(bitmap)
                                                                    .setBackgroundRegion(0,0, bitmap.getWidth() / 2, bitmap.getHeight())
                                                                    .setForegroundRegion(bitmap.getWidth() / 2, 0, bitmap.getWidth(), bitmap.getHeight())
                                                                    .setResizeBitmapArea(150 * 150)
                                                                    .getColors();
  ```
  
  Refer to [this link](https://stackoverflow.com/a/26253377/10750938) in order to know how positioning works.
  - You can use some of this presets: *.setLeftSide()*, *.setTopSide()*, *.setRightSide()*, *.setBottomSide()*. These presets will get the background color from the indicated side and the foreground colors from the opposite side.
  
  For example:
  ```
  HarmonicColors harmonicColors = new HarmonicColorExtractor.Builder.setBitmap(bitmap).setLeftSide().getColors();
  ```
  
### Based on Android
Much of the source code of the library has been obtained from the source code of Android. I only adapted it to have a simpler way to use it anywhere.
