package com.jiangdg.usbcamera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.common.UVCCameraHandler;
import com.serenegiant.usb.widget.ExternalCameraInterface;
import com.voxeet.sdk.external_usb.R;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * UVCCamera Helper class
 * <p>
 * Created by jiangdongguo on 2017/9/30.
 */
public class UVCCameraHelper {

    private Handler handler = new Handler(Looper.getMainLooper());
    public static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + File.separator;
    public static final String SUFFIX_JPEG = ".jpg";
    public static final String SUFFIX_MP4 = ".mp4";
    private static final String TAG = "UVCCameraHelper";
    private int previewWidth = 640;
    private int previewHeight = 480;
    public static final int FRAME_FORMAT_YUYV = UVCCamera.FRAME_FORMAT_YUYV;
    // Default using MJPEG
    // if your device is connected,but have no images
    // please try to change it to FRAME_FORMAT_YUYV
    public static final int FRAME_FORMAT_MJPEG = UVCCamera.FRAME_FORMAT_MJPEG;
    public static final int MODE_BRIGHTNESS = UVCCamera.PU_BRIGHTNESS;
    public static final int MODE_CONTRAST = UVCCamera.PU_CONTRAST;
    private int mFrameFormat = FRAME_FORMAT_MJPEG;

    private static UVCCameraHelper mCameraHelper;
    // USB Manager
    private USBMonitor mUSBMonitor;
    // Camera Handler
    private UVCCameraHandler mCameraHandler;
    private USBMonitor.UsbControlBlock mCtrlBlock;

    private Activity mActivity;
    private ExternalCameraInterface mCamView;

    private UVCCameraHelper() {
    }

    public static UVCCameraHelper getInstance() {
        if (mCameraHelper == null) {
            mCameraHelper = new UVCCameraHelper();
        }
        return mCameraHelper;
    }

    public void closeCamera() {
        if (mCameraHandler != null) {
            mCameraHandler.close();
        }
    }

    public interface OnMyDevConnectListener {
        void onAttachDev(UsbDevice device);

        void onDettachDev(UsbDevice device);

        void onConnectDev(UsbDevice device, boolean isConnected);

        void onDisConnectDev(UsbDevice device);
    }

    public void initUSBMonitor(Activity activity, ExternalCameraInterface cameraView, final OnMyDevConnectListener listener) {
        this.mActivity = activity;
        this.mCamView = cameraView;

        mUSBMonitor = new USBMonitor(activity.getApplicationContext(), new USBMonitor.OnDeviceConnectListener() {

            // called by checking usb device
            // do request device permission
            @Override
            public void onAttach(UsbDevice device) {
                if (listener != null) {
                    listener.onAttachDev(device);
                }
            }

            // called by taking out usb device
            // do close camera
            @Override
            public void onDettach(UsbDevice device) {
                if (listener != null) {
                    listener.onDettachDev(device);
                }
            }

            // called by connect to usb camera
            // do open camera,start previewing
            @Override
            public void onConnect(final UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock, boolean createNew) {
                mCtrlBlock = ctrlBlock;
                openCamera(ctrlBlock);
                // wait for camera created
                handler.postDelayed(() -> {
                    // start previewing
                    startPreview(mCamView);
                }, 500);
                if (listener != null) {
                    listener.onConnectDev(device, true);
                }
            }

            // called by disconnect to usb camera
            // do nothing
            @Override
            public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock ctrlBlock) {
                if (listener != null) {
                    listener.onDisConnectDev(device);
                }
            }

            @Override
            public void onCancel(UsbDevice device) {
            }
        });

        createUVCCamera();
    }

    public void createUVCCamera() {
        if (mCamView == null)
            throw new NullPointerException("CameraViewInterface cannot be null!");

        // release resources for initializing camera handler
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        // initialize camera handler
        mCameraHandler = UVCCameraHandler.createHandler(mActivity, mCamView, 2,
                previewWidth, previewHeight, mFrameFormat);
    }

    public void updateResolution(int width, int height) {
        if (previewWidth == width && previewHeight == height) {
            return;
        }
        this.previewWidth = width;
        this.previewHeight = height;
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        mCameraHandler = UVCCameraHandler.createHandler(mActivity, mCamView, 2,
                previewWidth, previewHeight, mFrameFormat);
        openCamera(mCtrlBlock);
        handler.postDelayed(() -> {
            // wait for camera created
            // start previewing
            startPreview(mCamView);
        }, 500);
    }

    public void registerUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.register();
        }
    }

    public void unregisterUSB() {
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    public boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    public int getModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    public int setModelValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    public int resetModelValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    public void requestPermission(int index, UsbDevice usbDevice) {
        List<UsbDevice> devList = getUsbDeviceList();

        if (null == usbDevice && (devList == null || devList.size() == 0)) {
            return;
        }
        int count = devList.size();
        if (null == usbDevice && index >= count)
            new IllegalArgumentException("index illegal,should be < devList.size()");
        if (mUSBMonitor != null) {
            mUSBMonitor.requestPermission(null == usbDevice ? getUsbDeviceList().get(index) : usbDevice);
        }
    }

    public int getUsbDeviceCount() {
        List<UsbDevice> devList = getUsbDeviceList();
        if (devList == null || devList.size() == 0) {
            return 0;
        }
        return devList.size();
    }

    public List<UsbDevice> getUsbDeviceList() {
        List<DeviceFilter> deviceFilters = DeviceFilter
                .getDeviceFilters(mActivity.getApplicationContext(), R.xml.device_filter);
        if (mUSBMonitor == null || deviceFilters == null)
            return null;
        return mUSBMonitor.getDeviceList(deviceFilters);
    }

    public void capturePicture(String savePath, AbstractUVCCameraHandler.OnCaptureListener listener) {
        if (mCameraHandler != null && mCameraHandler.isOpened()) {

            File file = new File(savePath);
            if (!Objects.requireNonNull(file.getParentFile()).exists()) {
                file.getParentFile().mkdirs();
            }
            mCameraHandler.captureStill(savePath, listener);
        }
    }

    public boolean isCameraOpened() {
        if (mCameraHandler != null) {
            return mCameraHandler.isOpened();
        }
        return false;
    }

    public void release() {
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }

    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    public void setOnPreviewFrameListener(AbstractUVCCameraHandler.OnPreViewResultListener listener) {
        if (mCameraHandler != null) {
            mCameraHandler.setOnPreViewResultListener(listener);
        }
    }

    private void openCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        if (mCameraHandler != null) {
            mCameraHandler.open(ctrlBlock);
        }
    }

    public void startPreview(ExternalCameraInterface cameraView) {
        SurfaceTexture st = cameraView.getSurfaceTexture();
        if (mCameraHandler != null) {
            mCameraHandler.startPreview(st);
        }
    }

    public void stopPreview() {
        if (mCameraHandler != null) {
            mCameraHandler.stopPreview();
        }
    }

    public void startCameraFocus() {
        if (mCameraHandler != null) {
            mCameraHandler.startCameraFoucs();
        }
    }

    public List<Size> getSupportedPreviewSizes() {
        if (mCameraHandler == null)
            return null;
        return mCameraHandler.getSupportedPreviewSizes();
    }

    public void setDefaultPreviewSize(int defaultWidth, int defaultHeight) {
        if (mUSBMonitor != null) {
            throw new IllegalStateException("setDefaultPreviewSize should be call before initMonitor");
        }
        this.previewWidth = defaultWidth;
        this.previewHeight = defaultHeight;
    }

    public void setDefaultFrameFormat(int format) {
        if (mUSBMonitor != null) {
            throw new IllegalStateException("setDefaultFrameFormat should be call before initMonitor");
        }
        this.mFrameFormat = format;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }
}
