package de.daubli.ndimonitor.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class VideoView extends TextureView implements TextureView.SurfaceTextureListener {

    private final Object lock = new Object();

    private Bitmap frontBuffer;
    private Bitmap backBuffer;

    private Rect dstRect;

    public VideoView(@NonNull Context context) {
        super(context);
        init();
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    public void updateFrame(Bitmap bitmap) {
        if (bitmap == null || !isAvailable()) return;

        int viewWidth = getWidth();
        int viewHeight = getHeight();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        // Maintain aspect ratio
        float viewAspect = (float) viewWidth / viewHeight;
        float bitmapAspect = (float) bitmapWidth / bitmapHeight;

        int dstWidth, dstHeight;
        if (bitmapAspect > viewAspect) {
            // Image is wider than view
            dstWidth = viewWidth;
            dstHeight = (int) (viewWidth / bitmapAspect);
        } else {
            // Image is taller than view
            dstHeight = viewHeight;
            dstWidth = (int) (viewHeight * bitmapAspect);
        }

        // Calculate centered position
        int left = (viewWidth - dstWidth) / 2;
        int top = (viewHeight - dstHeight) / 2;
        dstRect = new Rect(left, top, left + dstWidth, top + dstHeight);

        Canvas canvas = null;
        try {
            canvas = lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.BLACK); // Clear previous frame
                canvas.drawBitmap(bitmap, null, dstRect, null);
            }
        } finally {
            if (canvas != null) {
                unlockCanvasAndPost(canvas);
            }
        }
    }

    public Rect getLocationOnScreenRect() {
        return dstRect;
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        synchronized (lock) {
            if (frontBuffer != null) {
                frontBuffer.recycle();
                frontBuffer = null;
            }
            if (backBuffer != null) {
                backBuffer.recycle();
                backBuffer = null;
            }
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        // no-op
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        // no-op
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
        // no-op
    }
}
