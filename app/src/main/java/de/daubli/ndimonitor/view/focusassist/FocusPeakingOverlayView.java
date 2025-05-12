package de.daubli.ndimonitor.view.focusassist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

public class FocusPeakingOverlayView extends TextureView implements TextureView.SurfaceTextureListener {
    private FocusPeakingGLRenderer renderer;

    public FocusPeakingOverlayView(Context context) {
        super(context);
        init();
    }

    public FocusPeakingOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
        setOpaque(false);
    }

    public void updateFrame(Bitmap bitmap, Rect dstRect) {
        if (renderer != null) {
            renderer.setFrame(bitmap, dstRect);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        renderer = new FocusPeakingGLRenderer(surface);
        renderer.start();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (renderer != null) {
            renderer.shutdown();
            renderer = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
