package de.daubli.ndimonitor.view.video.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BitmapVideoView extends SurfaceView implements SurfaceHolder.Callback {
    
    private Rect dstRect;

    private boolean surfaceReady = false;

    public BitmapVideoView(@NonNull Context context) {
        super(context);
        init();
    }

    public BitmapVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        getHolder().setFormat(android.graphics.PixelFormat.OPAQUE);
    }

    public void updateFrame(Bitmap bitmap) {
        if (bitmap == null || !surfaceReady) {
            return;
        }

        final int viewWidth = getWidth();
        final int viewHeight = getHeight();
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        if (viewWidth == 0 || viewHeight == 0) {
            return;
        }

        // Maintain aspect ratio
        float viewAspect = (float) viewWidth / viewHeight;
        float bitmapAspect = (float) bitmapWidth / bitmapHeight;

        int dstWidth, dstHeight;
        if (bitmapAspect > viewAspect) {
            dstWidth = viewWidth;
            dstHeight = (int) (viewWidth / bitmapAspect);
        } else {
            dstHeight = viewHeight;
            dstWidth = (int) (viewHeight * bitmapAspect);
        }

        int left = (viewWidth - dstWidth) / 2;
        int top = (viewHeight - dstHeight) / 2;
        dstRect = new Rect(left, top, left + dstWidth, top + dstHeight);

        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.BLACK);
                canvas.drawBitmap(bitmap, null, dstRect, null);
            }
        } finally {
            if (canvas != null) {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    public Rect getLocationOnScreenRect() {
        return dstRect;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceReady = true;
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        surfaceReady = false;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // no-op
    }
}
