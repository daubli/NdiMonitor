package de.daubli.ndimonitor.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import de.daubli.ndimonitor.render.BgraBitmapBuilder;
import de.daubli.ndimonitor.render.UyvyBitmapBuilder;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolayVideoFrame;

public class NdiVideoView extends View {

    private Bitmap currentFrameBitmapOld;
    private Bitmap currentFrameBitmap;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private int leftOffset = 0;

    public NdiVideoView(Context context) {
        super(context);
    }

    public NdiVideoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        if (currentFrameBitmapOld != null) {
            canvas.drawBitmap(currentFrameBitmapOld, leftOffset, 0, null);
        }

        if (currentFrameBitmap != null) {
            canvas.drawBitmap(currentFrameBitmap, leftOffset, 0, paint);
        }

        canvas.restore();
    }

    public void deleteCurrentFrameData() {
        this.currentFrameBitmap = null;
        this.currentFrameBitmapOld = null;
    }

    public void setCurrentFrame(DevolayVideoFrame videoFrame, boolean isFirstFrame) {
        int heightOfView = this.getHeight();
        int widthOfView = this.getWidth();

        // landscape view
        float scalingFactor = heightOfView > 0 ? (float) videoFrame.getYResolution() / heightOfView : 1;
        int height = (int) (videoFrame.getYResolution() / scalingFactor);
        int width = (int) (videoFrame.getXResolution() / scalingFactor);

        leftOffset = (widthOfView - width) / 2;

        if (videoFrame.getFourCCType().equals(DevolayFrameFourCCType.UYVY)) {
            this.currentFrameBitmap =
                    UyvyBitmapBuilder.builder().withFrame(videoFrame).withHeight(height).withWidth(width).build();
        }

        if (videoFrame.getFourCCType().equals(DevolayFrameFourCCType.BGRA)) {
            this.currentFrameBitmap =
                    BgraBitmapBuilder.builder().withFrame(videoFrame).withHeight(height).withWidth(width).build();
        }
        if (!isFirstFrame) {
            this.currentFrameBitmapOld = currentFrameBitmap;
        }

        this.invalidate();
    }
}
