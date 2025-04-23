package de.daubli.ndimonitor.decoder;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import de.daubli.ndimonitor.ndi.FourCCType;
import de.daubli.ndimonitor.ndi.NdiVideoFrame;

public class NdiFrameDecoder {
    @Nullable
    public static Bitmap decode(NdiVideoFrame ndiVideoFrame, int heightOfView) {
        // landscape view
        float scalingFactor = heightOfView > 0 ? (float) ndiVideoFrame.getYResolution() / heightOfView : 1;
        int targetHeight = (int) (ndiVideoFrame.getYResolution() / scalingFactor);
        int targetWidth = (int) (ndiVideoFrame.getXResolution() / scalingFactor);

        if (ndiVideoFrame.getFourCCType().equals(FourCCType.UYVY)) {
            return UyvyBitmapBuilder.builder()
                    .withFrame(ndiVideoFrame)
                    .withHeight(targetHeight)
                    .withWidth(targetWidth)
                    .build();
        } else if (ndiVideoFrame.getFourCCType().equals(FourCCType.BGRA)) {
            return BgraBitmapBuilder.builder()
                    .withFrame(ndiVideoFrame)
                    .withHeight(targetHeight)
                    .withWidth(targetWidth)
                    .build();
        } else {
            return null;
        }
    }
}
