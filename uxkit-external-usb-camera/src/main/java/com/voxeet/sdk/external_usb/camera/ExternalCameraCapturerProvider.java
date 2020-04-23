package com.voxeet.sdk.external_usb.camera;

import android.app.Activity;
import android.graphics.Bitmap;

import com.voxeet.android.media.utils.VideoCapturerProvider;
import com.voxeet.promise.Promise;

import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoCapturer;

import javax.annotation.Nullable;

public class ExternalCameraCapturerProvider implements VideoCapturerProvider {

    private Activity activity;
    private ExternalCameraCapturer capturer;

    public ExternalCameraCapturerProvider(Activity activity) {
        this.activity = activity;
    }

    @Nullable
    @Override
    public ExternalCameraCapturer createVideoCapturer(@Nullable String s) {
        capturer = new ExternalCameraCapturer(activity, getWidth(), getHeight());
        return capturer;
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

    public boolean hasCapturer() {
        return null != capturer;
    }

    /**
     * Get the next bitmap. The promise will timeout after 5s.
     * The capturer session must have started
     *
     * @param width  the expected width, should match the provider's
     * @param height the expected height, should match the provider's
     * @return a promise to resolve.
     */
    public Promise<Bitmap> toBitmap(int width, int height) {
        if (null == capturer)
            return Promise.reject(new IllegalStateException("Can't call toBitmap"));
        return capturer.toBitmap(width, height);
    }
}
