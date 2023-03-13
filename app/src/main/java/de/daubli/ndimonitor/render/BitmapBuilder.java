package de.daubli.ndimonitor.render;

import static io.github.crow_misia.libyuv.AbgrBuffer.Factory;

import android.graphics.Bitmap;
import io.github.crow_misia.libyuv.AbgrBuffer;
import io.github.crow_misia.libyuv.ArgbBuffer;
import me.walkerknapp.devolay.DevolayVideoFrame;

public abstract class BitmapBuilder {

    int width = 0;
    int height = 0;
    DevolayVideoFrame frame;

    public BitmapBuilder withWidth(int width) {
        this.width = width;
        return this;
    }

    public BitmapBuilder withHeight(int height) {
        this.height = height;
        return this;
    }

    public BitmapBuilder withFrame(DevolayVideoFrame frame) {
        this.frame = frame;
        return this;
    }

    Bitmap buildScaledBitmapFromArgbBuffer(ArgbBuffer argbBuffer) {
        AbgrBuffer forBitmapBuffer = Factory.allocate(frame.getXResolution(), frame.getYResolution());
        argbBuffer.convertTo(forBitmapBuffer);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(forBitmapBuffer.asBitmap(), width, height, false);
        forBitmapBuffer.close();

        return scaledBitmap;
    }

    public abstract Bitmap build();
}
