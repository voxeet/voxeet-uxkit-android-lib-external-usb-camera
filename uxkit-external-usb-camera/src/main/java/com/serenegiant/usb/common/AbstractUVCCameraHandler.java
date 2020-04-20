package com.serenegiant.usb.common;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.widget.ExternalCameraInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Camera业务处理抽象类
 */
public abstract class AbstractUVCCameraHandler extends Handler {

    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "AbsUVCCameraHandler";


    // 对外回调接口
    public interface CameraCallback {
        public void onOpen();

        public void onClose();

        public void onStartPreview();

        public void onStopPreview();

        public void onStartRecording();

        public void onStopRecording();

        public void onError(final Exception e);
    }

    public static OnEncodeResultListener mListener;
    public static OnPreViewResultListener mPreviewListener;
    public static OnCaptureListener mCaptureListener;

    public interface OnEncodeResultListener {
        void onEncodeResult(byte[] data, int offset, int length, long timestamp, int type);

        void onRecordResult(String videoPath);
    }

    public interface OnPreViewResultListener {
        void onPreviewResult(byte[] data);
    }

    public interface OnCaptureListener {
        void onCaptureResult(String picPath);
    }

    private static final int MSG_OPEN = 0;
    private static final int MSG_CLOSE = 1;
    private static final int MSG_PREVIEW_START = 2;
    private static final int MSG_PREVIEW_STOP = 3;
    private static final int MSG_CAPTURE_STILL = 4;
    private static final int MSG_MEDIA_UPDATE = 7;
    private static final int MSG_RELEASE = 9;
    private static final int MSG_CAMERA_FOUCS = 10;
    // 音频线程
//	private static final int MSG_AUDIO_START = 10;
//	private static final int MSG_AUDIO_STOP = 11;

    private final WeakReference<CameraThread> mWeakThread;
    private volatile boolean mReleased;
    protected static boolean isCaptureStill;

    protected AbstractUVCCameraHandler(final CameraThread thread) {
        mWeakThread = new WeakReference<CameraThread>(thread);
    }

    public int getWidth() {
        final CameraThread thread = mWeakThread.get();
        return thread != null ? thread.getWidth() : 0;
    }

    public int getHeight() {
        final CameraThread thread = mWeakThread.get();
        return thread != null ? thread.getHeight() : 0;
    }

    public boolean isOpened() {
        final CameraThread thread = mWeakThread.get();
        return thread != null && thread.isCameraOpened();
    }

    public boolean isPreviewing() {
        final CameraThread thread = mWeakThread.get();
        return thread != null && thread.isPreviewing();
    }

    protected boolean isCameraThread() {
        final CameraThread thread = mWeakThread.get();
        return thread != null && (thread.getId() == Thread.currentThread().getId());
    }

    protected boolean isReleased() {
        final CameraThread thread = mWeakThread.get();
        return mReleased || (thread == null);
    }

    protected void checkReleased() {
        if (isReleased()) {
            throw new IllegalStateException("already released");
        }
    }

    public void open(final USBMonitor.UsbControlBlock ctrlBlock) {
        checkReleased();
        sendMessage(obtainMessage(MSG_OPEN, ctrlBlock));
    }

    public void close() {
        if (DEBUG) Log.v(TAG, "close:");
        if (isOpened()) {
            stopPreview();
            sendEmptyMessage(MSG_CLOSE);
        }
        if (DEBUG) Log.v(TAG, "close:finished");
    }

    // 切换分辨率
    public void resize(final int width, final int height) {
        checkReleased();
        throw new UnsupportedOperationException("does not support now");
    }

    // 开启Camera预览
    public void startPreview(final Object surface) {
        checkReleased();
        if (!((surface instanceof SurfaceHolder) || (surface instanceof Surface) || (surface instanceof SurfaceTexture))) {
            throw new IllegalArgumentException("surface should be one of SurfaceHolder, Surface or SurfaceTexture: " + surface);
        }

        sendMessage(obtainMessage(MSG_PREVIEW_START, surface));
    }

    public void setOnPreViewResultListener(OnPreViewResultListener listener) {
        AbstractUVCCameraHandler.mPreviewListener = listener;
    }

    // 关闭Camera预览
    public void stopPreview() {
        if (DEBUG) Log.v(TAG, "stopPreview:");
        removeMessages(MSG_PREVIEW_START);
        if (isPreviewing()) {
            final CameraThread thread = mWeakThread.get();
            if (thread == null) return;
            synchronized (thread.mSync) {
                sendEmptyMessage(MSG_PREVIEW_STOP);
                if (!isCameraThread()) {
                    // wait for actually preview stopped to avoid releasing Surface/SurfaceTexture
                    // while preview is still running.
                    // therefore this method will take a time to execute
                    try {
                        thread.mSync.wait();
                    } catch (final InterruptedException e) {
                    }
                }
            }
        }
        if (DEBUG) Log.v(TAG, "stopPreview:finished");
    }

    public void captureStill(final String path, AbstractUVCCameraHandler.OnCaptureListener listener) {
        AbstractUVCCameraHandler.mCaptureListener = listener;
        checkReleased();
        sendMessage(obtainMessage(MSG_CAPTURE_STILL, path));
        isCaptureStill = true;
    }

    public void startCameraFoucs() {
        sendEmptyMessage(MSG_CAMERA_FOUCS);
    }

    public List<Size> getSupportedPreviewSizes() {
        return mWeakThread.get().getSupportedSizes();
    }

//	// 启动音频线程
//	public void startAudioThread(){
//		sendEmptyMessage(MSG_AUDIO_START);
//	}
//
//	// 关闭音频线程
//	public void stopAudioThread(){
//		sendEmptyMessage(MSG_AUDIO_STOP);
//	}

    public void release() {
        mReleased = true;
        close();
        sendEmptyMessage(MSG_RELEASE);
    }

    // 对外注册监听事件
    public void addCallback(final CameraCallback callback) {
        checkReleased();
        if (!mReleased && (callback != null)) {
            final CameraThread thread = mWeakThread.get();
            if (thread != null) {
                thread.mCallbacks.add(callback);
            }
        }
    }

    public void removeCallback(final CameraCallback callback) {
        if (callback != null) {
            final CameraThread thread = mWeakThread.get();
            if (thread != null) {
                thread.mCallbacks.remove(callback);
            }
        }
    }

    protected void updateMedia(final String path) {
        sendMessage(obtainMessage(MSG_MEDIA_UPDATE, path));
    }

    public boolean checkSupportFlag(final long flag) {
        checkReleased();
        final CameraThread thread = mWeakThread.get();
        return thread != null && thread.mUVCCamera != null && thread.mUVCCamera.checkSupportFlag(flag);
    }

    public int getValue(final int flag) {
        checkReleased();
        final CameraThread thread = mWeakThread.get();
        final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
        if (camera != null) {
            if (flag == UVCCamera.PU_BRIGHTNESS) {
                return camera.getBrightness();
            } else if (flag == UVCCamera.PU_CONTRAST) {
                return camera.getContrast();
            }
        }
        throw new IllegalStateException();
    }

    public int setValue(final int flag, final int value) {
        checkReleased();
        final CameraThread thread = mWeakThread.get();
        final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
        if (camera != null) {
            if (flag == UVCCamera.PU_BRIGHTNESS) {
                camera.setBrightness(value);
                return camera.getBrightness();
            } else if (flag == UVCCamera.PU_CONTRAST) {
                camera.setContrast(value);
                return camera.getContrast();
            }
        }
        throw new IllegalStateException();
    }

    public int resetValue(final int flag) {
        checkReleased();
        final CameraThread thread = mWeakThread.get();
        final UVCCamera camera = thread != null ? thread.mUVCCamera : null;
        if (camera != null) {
            if (flag == UVCCamera.PU_BRIGHTNESS) {
                camera.resetBrightness();
                return camera.getBrightness();
            } else if (flag == UVCCamera.PU_CONTRAST) {
                camera.resetContrast();
                return camera.getContrast();
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public void handleMessage(final Message msg) {
        final CameraThread thread = mWeakThread.get();
        if (thread == null) return;
        switch (msg.what) {
            case MSG_OPEN:
                thread.handleOpen((USBMonitor.UsbControlBlock) msg.obj);
                break;
            case MSG_CLOSE:
                thread.handleClose();
                break;
            case MSG_PREVIEW_START:
                thread.handleStartPreview(msg.obj);
                break;
            case MSG_PREVIEW_STOP:
                thread.handleStopPreview();
                break;
            case MSG_CAPTURE_STILL:
//				thread.handleCaptureStill((String)msg.obj);
                thread.handleStillPicture((String) msg.obj);
                break;
            case MSG_MEDIA_UPDATE:
                thread.handleUpdateMedia((String) msg.obj);
                break;
            case MSG_RELEASE:
                thread.handleRelease();
                break;
            // 自动对焦
            case MSG_CAMERA_FOUCS:
                thread.handleCameraFoucs();
                break;
            default:
                throw new RuntimeException("unsupported message:what=" + msg.what);
        }
    }

    public static final class CameraThread extends Thread {
        private static final String TAG_THREAD = "CameraThread";
        private final Object mSync = new Object();
        private final Class<? extends AbstractUVCCameraHandler> mHandlerClass;
        private final WeakReference<Activity> mWeakParent;
        private final WeakReference<ExternalCameraInterface> mWeakCameraView;
        private final int mEncoderType;
        private final Set<CameraCallback> mCallbacks = new CopyOnWriteArraySet<CameraCallback>();
        private int mWidth, mHeight, mPreviewMode;
        private float mBandwidthFactor;
        private boolean mIsPreviewing;

        private AbstractUVCCameraHandler mHandler;
        // 处理与Camera相关的逻辑，比如获取byte数据流等
        private UVCCamera mUVCCamera;

        /**
         * 构造方法
         * <p>
         * clazz 继承于AbstractUVCCameraHandler
         * parent Activity子类
         * cameraView 用于捕获静止图像
         * encoderType 0表示使用MediaSurfaceEncoder;1表示使用MediaVideoEncoder, 2表示使用MediaVideoBufferEncoder
         * width  分辨率的宽
         * height 分辨率的高
         * format 颜色格式，0为FRAME_FORMAT_YUYV；1为FRAME_FORMAT_MJPEG
         * bandwidthFactor
         */
        CameraThread(final Class<? extends AbstractUVCCameraHandler> clazz,
                     final Activity parent, final ExternalCameraInterface cameraView,
                     final int encoderType, final int width, final int height, final int format,
                     final float bandwidthFactor) {

            super("CameraThread");
            mHandlerClass = clazz;
            mEncoderType = encoderType;
            mWidth = width;
            mHeight = height;
            mPreviewMode = format;
            mBandwidthFactor = bandwidthFactor;
            mWeakParent = new WeakReference<>(parent);
            mWeakCameraView = new WeakReference<>(cameraView);
//			loadShutterSound(parent);
        }

        @Override
        protected void finalize() throws Throwable {
            Log.i(TAG, "CameraThread#finalize");
            super.finalize();
        }

        public AbstractUVCCameraHandler getHandler() {
            if (DEBUG) Log.v(TAG_THREAD, "getHandler:");
            synchronized (mSync) {
                if (mHandler == null)
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                    }
            }
            return mHandler;
        }

        public int getWidth() {
            synchronized (mSync) {
                return mWidth;
            }
        }

        public int getHeight() {
            synchronized (mSync) {
                return mHeight;
            }
        }

        public boolean isCameraOpened() {
            synchronized (mSync) {
                return mUVCCamera != null;
            }
        }

        public boolean isPreviewing() {
            synchronized (mSync) {
                return mUVCCamera != null && mIsPreviewing;
            }
        }

        public void handleOpen(final USBMonitor.UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG_THREAD, "handleOpen:");
            handleClose();
            try {
                final UVCCamera camera = new UVCCamera();
                camera.open(ctrlBlock);
                synchronized (mSync) {
                    mUVCCamera = camera;
                }
                callOnOpen();
            } catch (final Exception e) {
                callOnError(e);
            }
            if (DEBUG)
                Log.i(TAG, "supportedSize:" + (mUVCCamera != null ? mUVCCamera.getSupportedSize() : null));
        }

        public void handleClose() {
            if (DEBUG) Log.v(TAG_THREAD, "handleClose:");
            final UVCCamera camera;
            synchronized (mSync) {
                camera = mUVCCamera;
                mUVCCamera = null;
            }
            if (camera != null) {
                camera.stopPreview();
                camera.destroy();
                callOnClose();
            }
        }

        public void handleStartPreview(final Object surface) {
            if (DEBUG) Log.v(TAG_THREAD, "handleStartPreview:");
            if ((mUVCCamera == null) || mIsPreviewing) return;
            try {
                mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 31, mPreviewMode, mBandwidthFactor);
                // 获取USB Camera预览数据，使用NV21颜色会失真
                // 无论使用YUV还是MPEG，setFrameCallback的设置效果一致
//				mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_NV21);
                mUVCCamera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_YUV420SP);
            } catch (final IllegalArgumentException e) {
                try {
                    // fallback to YUV mode
                    mUVCCamera.setPreviewSize(mWidth, mHeight, 1, 31, UVCCamera.DEFAULT_PREVIEW_MODE, mBandwidthFactor);
                } catch (final IllegalArgumentException e1) {
                    callOnError(e1);
                    return;
                }
            }
            if (surface instanceof SurfaceHolder) {
                mUVCCamera.setPreviewDisplay((SurfaceHolder) surface);
            }
            if (surface instanceof Surface) {
                mUVCCamera.setPreviewDisplay((Surface) surface);
            } else {
                mUVCCamera.setPreviewTexture((SurfaceTexture) surface);
            }
            mUVCCamera.startPreview();
            mUVCCamera.updateCameraParams();
            synchronized (mSync) {
                mIsPreviewing = true;
            }
            callOnStartPreview();
        }

        public void handleStopPreview() {
            if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:");
            if (mIsPreviewing) {
                if (mUVCCamera != null) {
                    mUVCCamera.stopPreview();
                    mUVCCamera.setFrameCallback(null, 0);
                }
                synchronized (mSync) {
                    mIsPreviewing = false;
                    mSync.notifyAll();
                }
                callOnStopPreview();
            }
            if (DEBUG) Log.v(TAG_THREAD, "handleStopPreview:finished");
        }

        private String picPath = null;

        public void handleStillPicture(String picPath) {
            this.picPath = picPath;
        }

        private final IFrameCallback mIFrameCallback = new IFrameCallback() {
            @Override
            public void onFrame(final ByteBuffer frame) {
                int len = frame.capacity();
                final byte[] yuv = new byte[len];
                frame.get(yuv);
                // nv21 yuv data callback
                if (mPreviewListener != null) {
                    mPreviewListener.onPreviewResult(yuv);
                }
                // picture
                if (isCaptureStill && !TextUtils.isEmpty(picPath)) {
                    isCaptureStill = false;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            saveYuv2Jpeg(picPath, yuv);
                        }
                    }).start();
                }
            }
        };

        private void saveYuv2Jpeg(String path, byte[] data) {
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
            boolean result = yuvImage.compressToJpeg(new Rect(0, 0, mWidth, mHeight), 100, bos);
            if (result) {

                byte[] buffer = bos.toByteArray();
                File file = new File(path);
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    // fixing bm is null bug instead of using BitmapFactory.decodeByteArray
                    fos.write(buffer);
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mCaptureListener != null) {
                    mCaptureListener.onCaptureResult(path);
                }
            }
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void handleUpdateMedia(final String path) {
            if (DEBUG) Log.v(TAG_THREAD, "handleUpdateMedia:path=" + path);
            final Activity parent = mWeakParent.get();
            final boolean released = (mHandler == null) || mHandler.mReleased;
            if (parent != null && parent.getApplicationContext() != null) {
                try {
                    if (DEBUG) Log.i(TAG, "MediaScannerConnection#scanFile");
                    MediaScannerConnection.scanFile(parent.getApplicationContext(), new String[]{path}, null, null);
                } catch (final Exception e) {
                    Log.e(TAG, "handleUpdateMedia:", e);
                }
                if (released || parent.isDestroyed())
                    handleRelease();
            } else {
                Log.w(TAG, "MainActivity already destroyed");
                // give up to add this movie to MediaStore now.
                // Seeing this movie on Gallery app etc. will take a lot of time.
                handleRelease();
            }
        }

        public void handleRelease() {
            handleClose();
            mCallbacks.clear();
            mHandler.mReleased = true;
            Looper.myLooper().quit();
            if (DEBUG) Log.v(TAG_THREAD, "handleRelease:finished");
        }

        // 自动对焦
        public void handleCameraFoucs() {
            if (DEBUG) Log.v(TAG_THREAD, "handleStartPreview:");
            if ((mUVCCamera == null) || !mIsPreviewing)
                return;
            mUVCCamera.setAutoFocus(true);
        }

        // 获取支持的分辨率
        public List<Size> getSupportedSizes() {
            if ((mUVCCamera == null) || !mIsPreviewing)
                return null;
            return mUVCCamera.getSupportedSizeList();
        }

        @Override
        public void run() {
            Looper.prepare();
            AbstractUVCCameraHandler handler = null;
            try {
                final Constructor<? extends AbstractUVCCameraHandler> constructor = mHandlerClass.getDeclaredConstructor(CameraThread.class);
                handler = constructor.newInstance(this);
            } catch (final NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                Log.w(TAG, e);
            }
            if (handler != null) {
                synchronized (mSync) {
                    mHandler = handler;
                    mSync.notifyAll();
                }
                Looper.loop();
                if (mHandler != null) {
                    mHandler.mReleased = true;
                }
            }
            mCallbacks.clear();
            synchronized (mSync) {
                mHandler = null;
                mSync.notifyAll();
            }
        }

        private void callOnOpen() {
            for (final CameraCallback callback : mCallbacks) {
                try {
                    callback.onOpen();
                } catch (final Exception e) {
                    mCallbacks.remove(callback);
                    Log.w(TAG, e);
                }
            }
        }

        private void callOnClose() {
            for (final CameraCallback callback : mCallbacks) {
                try {
                    callback.onClose();
                } catch (final Exception e) {
                    mCallbacks.remove(callback);
                    Log.w(TAG, e);
                }
            }
        }

        private void callOnStartPreview() {
            for (final CameraCallback callback : mCallbacks) {
                try {
                    callback.onStartPreview();
                } catch (final Exception e) {
                    mCallbacks.remove(callback);
                    Log.w(TAG, e);
                }
            }
        }

        private void callOnStopPreview() {
            for (final CameraCallback callback : mCallbacks) {
                try {
                    callback.onStopPreview();
                } catch (final Exception e) {
                    mCallbacks.remove(callback);
                    Log.w(TAG, e);
                }
            }
        }

        private void callOnStartRecording() {
            for (final CameraCallback callback : mCallbacks) {
                try {
                    callback.onStartRecording();
                } catch (final Exception e) {
                    mCallbacks.remove(callback);
                    Log.w(TAG, e);
                }
            }
        }

        private void callOnStopRecording() {
            for (final CameraCallback callback : mCallbacks) {
                try {
                    callback.onStopRecording();
                } catch (final Exception e) {
                    mCallbacks.remove(callback);
                    Log.w(TAG, e);
                }
            }
        }

        private void callOnError(final Exception e) {
            for (final CameraCallback callback : mCallbacks) {
                try {
                    callback.onError(e);
                } catch (final Exception e1) {
                    mCallbacks.remove(callback);
                    Log.w(TAG, e);
                }
            }
        }
    }
}
