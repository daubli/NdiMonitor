package de.daubli.ndimonitor.render;

import android.graphics.Bitmap;
import io.github.crow_misia.libyuv.ArgbBuffer;
import io.github.crow_misia.libyuv.UyvyBuffer;

public class UyvyBitmapBuilder extends BitmapBuilder {

    public static UyvyBitmapBuilder builder() {
        return new UyvyBitmapBuilder();
    }

    @Override
    public Bitmap build() {
        int width = frame.getXResolution();
        int height = frame.getYResolution();

        UyvyBuffer uyvyBuffer = UyvyBuffer.Factory.wrap(frame.getData(), width, height);

        ArgbBuffer argbBuffer = ArgbBuffer.Factory.allocate(width, height);
        uyvyBuffer.convertTo(argbBuffer);
        uyvyBuffer.close();

        Bitmap resultBitmap = buildScaledBitmapFromArgbBuffer(argbBuffer);
        argbBuffer.close();

        return resultBitmap;
    }
}
