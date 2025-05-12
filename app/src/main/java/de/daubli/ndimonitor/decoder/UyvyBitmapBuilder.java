package de.daubli.ndimonitor.decoder;

import android.graphics.Bitmap;
import io.github.crow_misia.libyuv.*;

public class UyvyBitmapBuilder extends BitmapBuilder {

    public static UyvyBitmapBuilder builder() {
        return new UyvyBitmapBuilder();
    }

    @Override
    public Bitmap build() {
        int frameWidth = frame.getXResolution();
        int frameHeight = frame.getYResolution();

        UyvyBuffer uyvyBuffer = UyvyBuffer.Factory.wrap(frame.getData(), frameWidth, frameHeight);

        I420Buffer i420Buffer = I420Buffer.Factory.allocate(frameWidth, frameHeight);
        uyvyBuffer.convertTo(i420Buffer);
        uyvyBuffer.close();

        I420Buffer scaledBuffer = I420Buffer.Factory.allocate(this.width, this.height);
        i420Buffer.scale(scaledBuffer, FilterMode.LINEAR);
        i420Buffer.close();

        ArgbBuffer argbBuffer = ArgbBuffer.Factory.allocate(this.width, this.height);
        scaledBuffer.convertTo(argbBuffer);
        scaledBuffer.close();

        Bitmap resultBitmap = buildBitmapFromArgbBuffer(argbBuffer);
        argbBuffer.close();

        return resultBitmap;
    }
}
