package com.voxeet.sdk.external_usb.camera;

import android.app.Activity;

import com.voxeet.android.media.utils.VideoCapturerProvider;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoCapturer;

import javax.annotation.Nullable;

public class ExternalCameraCapturerProvider implements VideoCapturerProvider {

    private Activity activity;

    public ExternalCameraCapturerProvider(Activity activity) {
        this.activity = activity;
    }

    @Nullable
    @Override
    public VideoCapturer createVideoCapturer(@Nullable String s) {
        return new ExternalCameraCapturer(activity, getWidth(), getHeight());
    }

    @Override
    public int getWidth() {
        return 640;
    }

    @Override
    public int getHeight() {
        return 480;
    }

    @Override
    public int getFrameRate() {
        return 30;
    }

    @Override
    public void switchCamera(@Nullable CameraVideoCapturer.CameraSwitchHandler cameraSwitchHandler, VideoCapturer videoCapturer) {
        if (null != cameraSwitchHandler) cameraSwitchHandler.onCameraSwitchDone(true);
    }
}
