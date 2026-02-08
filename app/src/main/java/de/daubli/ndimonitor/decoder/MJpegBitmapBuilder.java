package de.daubli.ndimonitor.decoder;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import androidx.annotation.Nullable;

/*
    This class is only needed to create bitmaps out of a UVC stream. NDI does never deliver YUY2 Data
 */
public class MJpegBitmapBuilder {

    ByteBuffer rawData;

    public static MJpegBitmapBuilder builder() {
        return new MJpegBitmapBuilder();
    }

    public MJpegBitmapBuilder withRawData(ByteBuffer buffer) {
        this.rawData = buffer;
        return this;
    }

    @Nullable
    public Bitmap build() {
        if (rawData == null) {
            throw new IllegalStateException("No input data provided.");
        }

        return decodeMjpeg(this.rawData);
    }

    private Bitmap decodeMjpeg(ByteBuffer buffer) {
        if (buffer == null || !buffer.hasRemaining()) {
            return null;
        }
        
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

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
