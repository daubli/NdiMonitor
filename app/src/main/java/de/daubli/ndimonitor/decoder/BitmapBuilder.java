package de.daubli.ndimonitor.decoder;

import static io.github.crow_misia.libyuv.AbgrBuffer.Factory;

import android.graphics.Bitmap;
import de.daubli.ndimonitor.ndi.NdiVideoFrame;
import io.github.crow_misia.libyuv.AbgrBuffer;
import io.github.crow_misia.libyuv.ArgbBuffer;

import java.util.logging.Logger;

public abstract class BitmapBuilder {
    static Logger LOG = Logger.getLogger(BitmapBuilder.class.getSimpleName());
    int width = 0;
    int height = 0;
    NdiVideoFrame frame;

    public BitmapBuilder withWidth(int width) {
        this.width = width;
        return this;
    }

    public BitmapBuilder withHeight(int height) {
        this.height = height;
        return this;
    }

    public BitmapBuilder withFrame(NdiVideoFrame frame) {
        this.frame = frame;
        return this;
    }

    Bitmap buildBitmapFromArgbBuffer(ArgbBuffer argbBuffer) {
        AbgrBuffer forBitmapBuffer = Factory.allocate(width, height);
        argbBuffer.convertTo(forBitmapBuffer);

        Bitmap unsafeBitmap = forBitmapBuffer.asBitmap();
        Bitmap safeCopy = unsafeBitmap.copy(Bitmap.Config.ARGB_8888, false);
        forBitmapBuffer.close();

        return Bitmap.createBitmap(safeCopy);
    }

    public abstract Bitmap build();
}
