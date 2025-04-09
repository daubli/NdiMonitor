package de.daubli.ndimonitor.ndi;

import java.nio.ByteBuffer;

public class VideoFrame {

    private int xResolution;

    private int yResolution;

    public VideoFrame() {
    }

    public VideoFrame(int xResolution, int yResolution) {
        this.xResolution = xResolution;
        this.yResolution = yResolution;
    }


    public int getXResolution() {
        return xResolution;
    }

    public int getYResolution() {
        return yResolution;
    }

    public ByteBuffer getData() {
        return ByteBuffer.wrap(new byte[]{});
    }

    public FourCCType getFourCCType() {
        return FourCCType.UYVY;
    }

    public int getFrameRateN() {
        return 0;
    }

    public int getFrameRateD() {
        return 0;
    }

    public void close() {
        
    }
}
