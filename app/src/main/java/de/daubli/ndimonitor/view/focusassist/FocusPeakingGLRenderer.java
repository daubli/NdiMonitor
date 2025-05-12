package de.daubli.ndimonitor.view.focusassist;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.*;
import android.view.Surface;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;
import java.util.concurrent.atomic.AtomicBoolean;

public class FocusPeakingGLRenderer extends Thread implements GLSurfaceView.Renderer {

    private EGL10 egl;
    private javax.microedition.khronos.egl.EGLDisplay display;
    private EGLContext eglContext;
    private EGLConfig eglConfig;
    private final Surface surface;
    private FocusPeakingShader shader;
    private Bitmap frameBitmap;
    private Rect dstRect;
    private final AtomicBoolean shouldRender = new AtomicBoolean(false);
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private EGLSurface eglSurface;

    public FocusPeakingGLRenderer(SurfaceTexture surfaceTexture) {
        this.surface = new Surface(surfaceTexture);
    }

    public void setFrame(Bitmap bitmap, Rect dstRect) {
        this.frameBitmap = bitmap;
        this.dstRect = dstRect;
        shouldRender.set(true);
    }

    public void shutdown() {
        isRunning.set(false);
    }

    @Override
    public void run() {
        initGL();
        shader = new FocusPeakingShader();
        while (isRunning.get()) {
            if (shouldRender.getAndSet(false) && frameBitmap != null && dstRect != null) {
                GLES20.glViewport(dstRect.left, dstRect.top, dstRect.width(), dstRect.height());
                shader.draw(frameBitmap);
                egl.eglSwapBuffers(display, eglSurface);
            }
        }
        deinitGL();
    }

    private void initGL() {
        egl = (EGL10) EGLContext.getEGL();
        display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        int[] configAttribs = {
                EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8, EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_NONE
        };
        javax.microedition.khronos.egl.EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(display, configAttribs, configs, 1, numConfig);
        eglConfig = configs[0];

        int[] contextAttribs = {0x3098, 2, EGL10.EGL_NONE}; // EGL_CONTEXT_CLIENT_VERSION
        eglContext = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, contextAttribs);
        eglSurface = egl.eglCreateWindowSurface(display, eglConfig, surface, null);
        egl.eglMakeCurrent(display, eglSurface, eglSurface, eglContext);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void deinitGL() {
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglDestroyContext(display, eglContext);
        egl.eglTerminate(display);
        surface.release();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
    }

    @Override
    public void onDrawFrame(GL10 gl) {
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eglConfig) {

    }
}
