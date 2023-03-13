package de.daubli.ndimonitor.render;

import android.graphics.Bitmap;
import io.github.crow_misia.libyuv.ArgbBuffer;

public class BgraBitmapBuilder extends BitmapBuilder {

    public static BgraBitmapBuilder builder() {
        return new BgraBitmapBuilder();
    }

    @Override
    public Bitmap build() {
        int width = frame.getXResolution();
        int height = frame.getYResolution();

        ArgbBuffer argbBuffer = ArgbBuffer.Factory.wrap(frame.getData(), width, height);
        Bitmap resultBitmap = buildScaledBitmapFromArgbBuffer(argbBuffer);
        argbBuffer.close();

        return resultBitmap;
    }
}
