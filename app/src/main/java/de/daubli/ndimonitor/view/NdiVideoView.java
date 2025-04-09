package de.daubli.ndimonitor.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import de.daubli.ndimonitor.ndi.FourCCType;
import de.daubli.ndimonitor.ndi.VideoFrame;
import de.daubli.ndimonitor.render.BgraBitmapBuilder;
import de.daubli.ndimonitor.render.UyvyBitmapBuilder;

public class NdiVideoView extends View {

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
        canvas.drawBitmap(currentFrameBitmap, leftOffset, 0, paint);
        canvas.restore();
    }

    public void deleteCurrentFrameData() {
        this.currentFrameBitmap = null;
    }

    public void setCurrentFrame(VideoFrame videoFrame) {
        int heightOfView = this.getHeight();
        int widthOfView = this.getWidth();

        // landscape view
        float scalingFactor = heightOfView > 0 ? (float) videoFrame.getYResolution() / heightOfView : 1;
        int height = (int) (videoFrame.getYResolution() / scalingFactor);
        int width = (int) (videoFrame.getXResolution() / scalingFactor);

        leftOffset = (widthOfView - width) / 2;

        if (videoFrame.getFourCCType().equals(FourCCType.UYVY)) {
            this.currentFrameBitmap =
                    UyvyBitmapBuilder.builder().withFrame(videoFrame).withHeight(height).withWidth(width).build();
        }

        if (videoFrame.getFourCCType().equals(FourCCType.BGRA)) {
            this.currentFrameBitmap =
                    BgraBitmapBuilder.builder().withFrame(videoFrame).withHeight(height).withWidth(width).build();
        }

        this.invalidate();
    }
}
