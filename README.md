## UXKit External USB Camera

Project forked from the Apache 2.0 Open Source AndroidUSBCamera project available [here](https://github.com/jiangdongguo/AndroidUSBCamera)

Supporting Android 5.0+

## Installation

```java
dependencies {
	implementation "com.voxeet.sdk:uxkit-external-usb-camera:0.0.1"
}

```

## Usage

_The following documentation reflect the current internal implementation of the ExternalCameraCapturerProvider()_

Internally

```java
UVCCameraHelper cameraHelper = UVCCameraHelper.getInstance();

cameraHelper.setDefaultPreviewSize(1280,720);

// set default frame format，defalut is UVCCameraHelper.Frame_FORMAT_MPEG
// if using mpeg can not record mp4,please try yuv
cameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_YUYV);
cameraHelper.initUSBMonitor(this, mUVCCameraView, mDevConnectListener);

//when starting capturing
cameraHelper.registerUSB();

//when stopping
cameraHelper.unregisterUSB();

```

TODO : add the wrapper on the capture :

```java
 mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        Log.i(TAG,"save path：" + path);
                    }
                }); 
```

TODO setting camera's brightness and contrast.
```java
mCameraHelper.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS,progress);
mCameraHelper.setModelValue(UVCCameraHelper.MODE_CONTRAST,progress);
mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS);
mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST);
...
```
![Connecting gif](https://github.com/jiangdongguo/AndroidUSBCamera/blob/master/gifs/brightness.gif)
(5) switch resolutions and camera.  
```java
mCameraHelper.updateResolution(widht, height);
```
![Connecting gif](https://github.com/jiangdongguo/AndroidUSBCamera/blob/master/gifs/2.1.0.gif)  
At last,remember adding permissions:  
```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />  
```


## Report issues

- connected but no preview

Please checking your preview format and change YUV to MJPEG or MJPEG to YUV, because some usb devices only supporting YUV

- device not found

Check the OTG feature on your device

Send the failed-device.txt in the /sdcard/UsbCamera/failed-device.txt


## Changelog

#### 2020/04/20

- Add External capturer based on the original complete implementation
- remove unecessary classes for this library's purpose. Those are still and will remain in the commit's hierarchy
- unbranch from original repository

License
-------

    Copyright 2020 Voxeet

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
