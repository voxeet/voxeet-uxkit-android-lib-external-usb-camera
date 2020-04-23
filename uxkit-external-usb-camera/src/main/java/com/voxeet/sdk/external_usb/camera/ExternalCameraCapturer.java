package com.voxeet.sdk.external_usb.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.widget.Toast;

import com.jiangdg.usbcamera.UVCCameraHelper;
import com.serenegiant.usb.widget.ExternalCameraInterface;
import com.voxeet.promise.Promise;
import com.voxeet.promise.solve.Solver;
import com.voxeet.promise.solve.ThenVoid;

import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

import javax.annotation.Nullable;

public class ExternalCameraCapturer implements VideoCapturer, VideoSink {
    private final static Handler handler = new Handler(Looper.getMainLooper());
    private final Activity activity;

    private int width;
    private int height;
    @Nullable
    private SurfaceTextureHelper surfaceTextureHelper;
    @Nullable
    private CapturerObserver capturerObserver;
    private long numCapturedFrames;
    private boolean isDisposed;
    private Solver<Bitmap> waitForFrameSolver = null;

    UVCCameraHelper cameraHelper = UVCCameraHelper.getInstance();
    private ExternalCameraInterface externalCameraInterface = new ExternalCameraInterface() {
        @Override
        public void onPause() {

        }

        @Override
        public void onResume() {

        }

        @Override
        public void setCallback(Callback callback) {

        }

        @Override
        public SurfaceTexture getSurfaceTexture() {
            return surfaceTextureHelper.getSurfaceTexture();
        }

        @Override
        public Surface getSurface() {
            return surface;
        }

        @Override
        public boolean hasSurface() {
            return null != surface;
        }
    };

    private Surface surface;
    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {
        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission(must have)
            if (!isRequest) {
                isRequest = true;
                if (cameraHelper != null) {
                    cameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera(must have)
            if (isRequest) {
                isRequest = false;
                cameraHelper.closeCamera();
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {

        }

        @Override
        public void onDisConnectDev(UsbDevice device) {

        }
    };
    private boolean isRequest = false;
    private Point waitForFrameSolverSize;

    public ExternalCameraCapturer(Activity activity, int width, int height) {
        cameraHelper.setDefaultPreviewSize(width, height);

        this.width = width;
        this.height = height;

        this.activity = activity;

        toast("constructor");
    }

    private void toast(String t) {
        Toast.makeText(activity, t, Toast.LENGTH_SHORT).show();
    }

    private void checkNotDisposed() {
        if (isDisposed) {
            throw new RuntimeException("capturer is disposed.");
        }
    }

    @Override
    public synchronized void initialize(final SurfaceTextureHelper surfaceTextureHelper,
                                        final Context applicationContext, final CapturerObserver capturerObserver) {
        toast("initialize");
        checkNotDisposed();

        if (capturerObserver == null) {
            throw new RuntimeException("capturerObserver not set.");
        }
        this.capturerObserver = capturerObserver;

        if (surfaceTextureHelper == null) {
            throw new RuntimeException("surfaceTextureHelper not set.");
        }
        this.surfaceTextureHelper = surfaceTextureHelper;
        this.surfaceTextureHelper.setTextureSize(width, height);

        //init something here
        this.surface = new Surface(surfaceTextureHelper.getSurfaceTexture());
    }

    @Override
    public synchronized void startCapture(
            final int width, final int height, final int ignoredFramerate) {
        toast("startCapture");
        checkNotDisposed();

        cameraHelper.initUSBMonitor(activity, externalCameraInterface, listener);
        cameraHelper.registerUSB();

        this.width = width;
        this.height = height;

        capturerObserver.onCapturerStarted(true);
        surfaceTextureHelper.startListening(this);
    }

    @Override
    public synchronized void stopCapture() {
        toast("stopCapturer");
        checkNotDisposed();
        ThreadUtils.invokeAtFrontUninterruptibly(surfaceTextureHelper.getHandler(), () -> {
            surfaceTextureHelper.stopListening();
            capturerObserver.onCapturerStopped();

            cameraHelper.stopPreview();
            cameraHelper.unregisterUSB();
            cameraHelper.release();
        });
    }

    @Override
    public synchronized void dispose() {
        cameraHelper.release();
        isDisposed = true;
    }

    @Override
    public synchronized void changeCaptureFormat(
            final int width, final int height, final int ignoredFramerate) {
        checkNotDisposed();

        this.width = width;
        this.height = height;
    }

    @Override
    public void onFrame(VideoFrame frame) {
        numCapturedFrames++;
        capturerObserver.onFrameCaptured(frame);

        if (null != frame && null != waitForFrameSolver) {
            Solver<Bitmap> temp = waitForFrameSolver;
            waitForFrameSolver = null;
            FrameBitmapHelper.toBitmap(frame, waitForFrameSolverSize.x, waitForFrameSolverSize.y, false)
                    .then((ThenVoid<Bitmap>) temp::resolve)
                    .error(temp::reject);
        }
    }

    @Override
    public boolean isScreencast() {
        return true;
    }

    public long getNumCapturedFrames() {
        return numCapturedFrames;
    }

    Promise<Bitmap> toBitmap(int width, int height) {
        if (null != waitForFrameSolver)
            return Promise.reject(new IllegalStateException("already waiting for frame"));

        return new Promise<>(solver -> {
            handler.postDelayed(() -> {
                if (null != waitForFrameSolver) waitForFrameSolver = null;
            }, 5000);

            waitForFrameSolverSize = new Point(width, height);
            waitForFrameSolver = solver;
        });
    }
}

