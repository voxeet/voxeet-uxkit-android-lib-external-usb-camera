package com.voxeet.sdk.external_usb.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.voxeet.promise.Promise;

import org.webrtc.GlRectDrawer;
import org.webrtc.GlTextureFrameBuffer;
import org.webrtc.GlUtil;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrameDrawer;

import java.nio.ByteBuffer;

public class FrameBitmapHelper {

    private static VideoFrameDrawer frameDrawer; //release in the future ?
    private static GlTextureFrameBuffer bitmapTextureFramebuffer;
    private static GlRectDrawer drawer = new GlRectDrawer();
    private static Matrix drawMatrix = new Matrix();

    /**
     * Transform a given VideoFrame to its Bitmap version
     *
     * @param videoFrame valid VideoFrame
     * @param width      the expected width
     * @param height     the expected height
     * @param mirror     mirroring the bitmap using the Y axis
     * @return a bitmap or null if an exception occured
     */
    @Nullable
    public static Promise<Bitmap> toBitmap(@NonNull VideoFrame videoFrame, int width, int height, boolean mirror) {
        return new Promise<>(solver -> {

            try {
                drawMatrix.reset();
                drawMatrix.preTranslate(0.5f, 0.5f);
                if (mirror)
                    drawMatrix.preScale(-1f, 1f);
                drawMatrix.preTranslate(-0.5f, -0.5f);


                if (null == frameDrawer) {
                    frameDrawer = new VideoFrameDrawer();
                }

                if (bitmapTextureFramebuffer == null) {
                    bitmapTextureFramebuffer = new GlTextureFrameBuffer(GLES20.GL_RGBA);
                }
                bitmapTextureFramebuffer.setSize(width, height);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bitmapTextureFramebuffer.getFrameBufferId());
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, bitmapTextureFramebuffer.getTextureId(), 0);

                GLES20.glClearColor(0 /* red */, 0 /* green */, 0 /* blue */, 0 /* alpha */);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                frameDrawer.drawFrame(videoFrame, drawer, drawMatrix, 0 /* viewportX */,
                        0 /* viewportY */, width, height);

                final ByteBuffer bitmapBuffer = ByteBuffer.allocateDirect(width * height * 4);
                GLES20.glViewport(0, 0, width, height);
                GLES20.glReadPixels(
                        0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bitmapBuffer);

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                GlUtil.checkNoGLES2Error("EglRenderer.notifyCallbacks");

                final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(bitmapBuffer);

                solver.resolve(bitmap);
            } catch (Exception e) {
                solver.reject(e);
            }
        });
    }
}
