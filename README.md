## UXKit External USB Camera

Project forked from the Apache 2.0 Open Source AndroidUSBCamera project available [here](https://github.com/jiangdongguo/AndroidUSBCamera)

Supporting Android 5.0+

## Installation

```java
dependencies {
	implementation "com.voxeet.sdk:uxkit-external-usb-camera:3.1.2"
}

```

## Usage in Voxeet's Context

After a conference have been joined, make sure the device is unplugged (currently no chcek for pre-attached device), start a custom screenShare using the following snippet.

Then plug in the device and validate the "use usb" dialog. After a short time, the frames will be displayed.

_note : when starting the screenshare, until the device is plugged in, no frames will be sent, it is then currently normal to have a screenshare session without anything displayed_

```java
Activity activity = /*direct activity reference, used for the PermissionScreen and provide Context information*/;
ExternalCameraCapturerProvider provider = new ExternalCameraCapturerProvider(activity);
```

```java
VoxeetSDK.screenShare().startCustomScreenShare(provider).then(result -> {
    //anything here
}).error(error -> {
    //anything here
});
```

## Usage (internal)

_The following documentation reflect the current internal implementation of the ExternalCameraCapturerProvider()_

It can and will probably diverge from the original project.

### toBitmap

It's possible to get the next frame in Bitmap format (warning : need to be recycled)
The call will timeout after 5s if no frames are received

Following the previously initialized `ExternalCameraCapturerProvider` instance
```java

ExternalCameraCapturerProvider provider = new ExternalCameraCapturerProvider(activity);
int width = provider.getWidth(); //recommended
int height = provider.getHeight(); //recommended

provider.toBitmap(width, height).then(bitmap -> {
    //anything here
}).error(error -> {
    //anything here
});
```


### Internally

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

```java
mCameraHelper.updateResolution(widht, height);
```

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```


## Report issues

- connected but no preview

Please checking your preview format and change YUV to MJPEG or MJPEG to YUV, because some usb devices only supporting YUV

- device not found

Check the OTG feature on your device

Send the failed-device.txt in the /sdcard/UsbCamera/failed-device.txt


## Changelog

#### 2021/04/15

- bump this library to SDK 3.1.2
- align version altogether
- fix an issue which prevented presenters to attach screenshare onto their own device

#### 2020/04/20

- Add External capturer based on the original complete implementation
- remove unecessary classes for this library's purpose. Those are still and will remain in the commit's hierarchy
- unbranch from original repository

## Contributing

For open source related reasons, the current repository states embeds the sources from the original UVCCamera repository to make sure everyone can check, compile and regenerate every binaries embedded in the library.

Contributing can be done via fork followed by pull requests.

To make sure a pull request won't be cancelled, it must follows those steps :

- the code quality should follow the standards IntelliJ/Android Studio format
- the code must be commented
- only one feature should be present in the mull request
- avoid modifications in a number of files too high

## License

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
