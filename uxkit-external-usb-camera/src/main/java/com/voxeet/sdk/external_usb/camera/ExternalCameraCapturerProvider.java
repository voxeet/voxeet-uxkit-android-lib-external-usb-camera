package com.voxeet.sdk.external_usb.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.voxeet.android.media.utils.VideoCapturerBundle;
import com.voxeet.android.media.utils.VideoCapturerProvider;
import com.voxeet.promise.Promise;

import org.webrtc.VideoCapturer;

import javax.annotation.Nullable;

public class ExternalCameraCapturerProvider extends VideoCapturerProvider {

    private Activity activity;
    private ExternalCameraCapturer capturer;

    public ExternalCameraCapturerProvider(Activity activity) {
        this.activity = activity;

        constraints = new Constraints(640, 480, 30);
    }

    @Nullable
    @Override
    public VideoCapturerBundle createVideoCapturer(@Nullable String optionalName) {
        capturer = new ExternalCameraCapturer(activity, constraints.width, constraints.height);

        //TODO SDK : let the bundle to be overriden for the expected class !
        VideoCapturerBundle bundle = new VideoCapturerBundle(null,
                constraints.width,
                constraints.height,
                constraints.frameRate);
        bundle.videoCapturer = capturer;

        return bundle;
    }

    @NonNull
    @Override
    public void onVideoCapturerDisposed(@NonNull VideoCapturer videoCapturer) {
        capturer = null;
    }

    @Override
    public boolean changeCaptureFormat(int width, int height, int fps) {
        return false;
    }

    @Override
    public void clear() {

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
