package de.daubli.ndimonitor.decoder;

import android.graphics.Bitmap;
import io.github.crow_misia.libyuv.ArgbBuffer;
import io.github.crow_misia.libyuv.FilterMode;
import io.github.crow_misia.libyuv.I420Buffer;
import io.github.crow_misia.libyuv.I422Buffer;

public class BgraBitmapBuilder extends BitmapBuilder {

    public static BgraBitmapBuilder builder() {
        return new BgraBitmapBuilder();
    }

    @Override
    public Bitmap build() {
        int frameWidth = frame.getXResolution();
        int frameHeight = frame.getYResolution();

        ArgbBuffer argbBuffer = ArgbBuffer.Factory.wrap(frame.getData(), frameWidth, frameHeight);
        I422Buffer i422Buffer = I422Buffer.Factory.allocate(frameWidth, frameHeight);

        argbBuffer.convertTo(i422Buffer);
        argbBuffer.close();

        I422Buffer scaledBuffer = I422Buffer.Factory.allocate(width, height);
        i422Buffer.scale(scaledBuffer, FilterMode.LINEAR);
        i422Buffer.close();

        ArgbBuffer scaledArgbBuffer = ArgbBuffer.Factory.allocate(width, height);
        scaledBuffer.convertTo(scaledArgbBuffer);
        scaledBuffer.close();

        Bitmap resultBitmap = buildBitmapFromArgbBuffer(scaledArgbBuffer);
        scaledArgbBuffer.close();

        return resultBitmap;
    }
}
