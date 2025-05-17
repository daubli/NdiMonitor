package de.daubli.ndimonitor.decoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import io.github.crow_misia.libyuv.ArgbBuffer;
import io.github.crow_misia.libyuv.FilterMode;
import io.github.crow_misia.libyuv.I420Buffer;

import java.nio.ByteBuffer;

/*
    This class is only needed to create bitmaps out of a UVC stream. NDI does never deliver YUY2 Data
 */
public class MJpegBitmapBuilder extends BitmapBuilder {

    public static MJpegBitmapBuilder builder() {
        return new MJpegBitmapBuilder();
    }

    @Override
    public Bitmap build() {
        if (rawData == null) {
            throw new IllegalStateException("No input data provided.");
        }

        Bitmap unscaledBitmap = decodeMjpeg(this.rawData);
        this.sourceWidth = unscaledBitmap.getWidth();
        this.sourceHeight = unscaledBitmap.getHeight();

        if (this.sourceWidth == this.width && this.sourceHeight == this.height) {
            return unscaledBitmap;
        }

        ByteBuffer unscaledBuffer = ByteBuffer.allocateDirect(sourceWidth * sourceHeight * 4); // 4 bytes per pixel
        unscaledBitmap.copyPixelsToBuffer(unscaledBuffer);
        unscaledBuffer.rewind();

        ArgbBuffer argbBuffer = ArgbBuffer.Factory.wrap(unscaledBuffer, sourceWidth, sourceHeight);
        unscaledBuffer.clear();

        I420Buffer i420Buffer = I420Buffer.Factory.allocate(this.sourceWidth, this.sourceHeight);
        argbBuffer.convertTo(i420Buffer);
        argbBuffer.close();

        I420Buffer scaledBuffer = I420Buffer.Factory.allocate(width, height);
        i420Buffer.scale(scaledBuffer, FilterMode.LINEAR);
        i420Buffer.close();

        ArgbBuffer scaledArgbBuffer = ArgbBuffer.Factory.allocate(width, height);
        scaledBuffer.convertTo(scaledArgbBuffer);
        scaledBuffer.close();

        Bitmap bitmap = buildBitmapFromArgbBuffer(scaledArgbBuffer);
        argbBuffer.close();

        return bitmap;
    }

    private Bitmap decodeMjpeg(ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining()) return null;

        // ByteBuffer → byte[]
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        // Start & Ende des JPEG im ByteArray suchen
        int start = findJpegSOI(data);
        int end = findJpegEOI(data);

        if (start >= 0 && end > start) {
            int length = end - start + 2;
            try {
                return BitmapFactory.decodeByteArray(data, start, length);
            } catch (Exception e) {
                Log.e("UvcCapture", "JPEG decode failed", e);
            }
        }

        Log.e("UvcCapture", "Could not find valid JPEG segment in ByteBuffer");
        return null;
    }


    private int findJpegSOI(byte[] data) {
        for (int i = 0; i < data.length - 1; i++) {
            if ((data[i] & 0xFF) == 0xFF && (data[i + 1] & 0xFF) == 0xD8) {
                return i;
            }
        }
        return -1;
    }

    private int findJpegEOI(byte[] data) {
        for (int i = data.length - 2; i >= 0; i--) {
            if ((data[i] & 0xFF) == 0xFF && (data[i + 1] & 0xFF) == 0xD9) {
                return i;
            }
        }
        return -1;
    }
}
